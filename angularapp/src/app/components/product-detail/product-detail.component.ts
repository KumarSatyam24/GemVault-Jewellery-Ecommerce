import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  JewelleryItem,
  ProductReview,
} from '../../models/jewellery-item.model';
import { Subscription } from 'rxjs';
import { CartService } from '../../services/cart.service';
import { JewelleryService } from '../../services/jewellery.service';
import { AuthService } from '../../services/auth.service';
import {
  handleProductImageError,
  PRODUCT_IMAGE_FALLBACK,
} from '../../config/product-image.config';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  item?: JewelleryItem;
  loading = true;
  error = '';
  message = '';
  reviews: ProductReview[] = [];
  reviewsLoading = false;
  reviewsError = '';
  similarItems: JewelleryItem[] = [];
  similarError = '';
  private routeSubscription?: Subscription;
  private pageRequests = new Subscription();
  private cartNoticeTimer?: ReturnType<typeof setTimeout>;
  fromWishlist = false;
  imageFallback = PRODUCT_IMAGE_FALLBACK;
  imageFailed = handleProductImageError;
  readonly stars = [1, 2, 3, 4, 5];
  tryOnPhoto?: File;
  tryOnPreview = '';
  tryOnResult = '';
  tryOnResultType = 'image/jpeg';
  tryOnLoading = false;
  tryOnError = '';
  tryOnNeedsLogin = false;

  get roundedRating(): number {
    return Math.round(this.item?.ratingAverage || 0);
  }
  form = this.fb.group({
    quantity: [1, [Validators.required, Validators.min(1)]],
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private jewellery: JewelleryService,
    private cart: CartService,
    public auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe((params) => {
      this.loadProduct(Number(params.get('id')));
    });
  }

  private loadProduct(id: number): void {
    this.pageRequests.unsubscribe();
    this.pageRequests = new Subscription();
    if (this.cartNoticeTimer) clearTimeout(this.cartNoticeTimer);
    if (this.tryOnPreview) URL.revokeObjectURL(this.tryOnPreview);
    if (this.tryOnResult) URL.revokeObjectURL(this.tryOnResult);
    this.item = undefined;
    this.loading = true;
    this.error = this.message = this.reviewsError = this.similarError = '';
    this.reviews = [];
    this.similarItems = [];
    this.tryOnPhoto = undefined;
    this.tryOnPreview = this.tryOnResult = this.tryOnError = '';
    this.tryOnLoading = this.tryOnNeedsLogin = false;
    this.form.reset({ quantity: 1 });
    this.fromWishlist =
      this.route.snapshot.queryParamMap.get('from') === 'wishlist';
    if (!Number.isInteger(id) || id < 1) {
      this.router.navigate(['/']);
      return;
    }
    this.pageRequests.add(
      this.jewellery.getItemById(id).subscribe({
        next: (item) => {
          this.item = item;
          this.loading = false;
          this.loadReviews();
          this.loadSimilarItems();
          if (this.auth.isCustomer() && item.itemId)
            this.loadSavedTryOn(item.itemId);
          this.form.controls.quantity.setValidators([
            Validators.required,
            Validators.min(1),
            Validators.max(item.stockQuantity),
            Validators.pattern(/^[0-9]+$/),
          ]);
          this.form.controls.quantity.updateValueAndValidity();
        },
        error: () => {
          this.error = 'This piece is no longer available.';
          this.loading = false;
        },
      }),
    );
  }

  loadReviews(): void {
    if (!this.item?.itemId) return;
    this.reviewsLoading = true;
    this.reviewsError = '';
    this.pageRequests.add(
      this.jewellery.getProductReviews(this.item.itemId).subscribe({
        next: (reviews) => {
          this.reviews = reviews;
          this.reviewsLoading = false;
        },
        error: () => {
          this.reviewsLoading = false;
          this.reviewsError = 'Reviews could not be loaded. Please try again.';
        },
      }),
    );
  }

  loadSimilarItems(): void {
    if (!this.item?.itemId) return;
    this.similarError = '';
    this.pageRequests.add(
      this.jewellery.getSimilarItems(this.item.itemId).subscribe({
        next: (items) => {
          this.similarItems = items;
        },
        error: () => {
          this.similarError = 'Similar pieces could not be loaded.';
        },
      }),
    );
  }

  onTryOnPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const photo = input.files?.[0];
    if (!photo) return;
    this.tryOnError = '';
    if (
      !['image/jpeg', 'image/png'].includes(photo.type) ||
      photo.size > 4 * 1024 * 1024
    ) {
      this.tryOnError = 'Choose a JPEG or PNG photo smaller than 4 MB.';
      input.value = '';
      return;
    }
    if (this.tryOnPreview) URL.revokeObjectURL(this.tryOnPreview);
    this.tryOnPhoto = photo;
    this.tryOnPreview = URL.createObjectURL(photo);
  }

  generateTryOn(): void {
    if (
      !this.item?.itemId ||
      !this.tryOnPhoto ||
      this.tryOnLoading ||
      this.tryOnNeedsLogin
    )
      return;
    this.tryOnLoading = true;
    this.tryOnError = '';
    const itemId = this.item.itemId;
    this.pageRequests.add(
      this.jewellery.generateTryOn(itemId, this.tryOnPhoto).subscribe({
        next: (image) => {
          this.setTryOnResult(image);
          this.tryOnLoading = false;
        },
        error: (response) => {
          this.tryOnLoading = false;
          if (response.status === 401) {
            this.requireTryOnLogin();
            return;
          }
          if (response.status === 0) {
            this.tryOnError =
              'GemVault could not reach the backend. Check that the server is running, then try again.';
            return;
          }
          if (response.status === 413) {
            this.tryOnError =
              'The photo is too large. Choose a JPEG or PNG smaller than 4 MB.';
            return;
          }
          if (response.error instanceof Blob) {
            response.error.text().then((body: string) => {
              if (this.item?.itemId !== itemId) return;
              try {
                this.tryOnError =
                  JSON.parse(body).message || 'Image generation failed.';
              } catch {
                this.tryOnError = `The server returned HTTP ${response.status}. Please try again.`;
              }
            });
          } else {
            this.tryOnError =
              response.error?.message ||
              `The server returned HTTP ${response.status}. Please try again.`;
          }
        },
      }),
    );
  }

  downloadTryOn(): void {
    if (!this.tryOnResult || !this.item) return;
    const anchor = document.createElement('a');
    anchor.href = this.tryOnResult;
    anchor.download = `gemvault-try-on-${this.item.itemId}.${this.tryOnResultType === 'image/png' ? 'png' : this.tryOnResultType === 'image/webp' ? 'webp' : 'jpg'}`;
    anchor.click();
  }

  removeTryOn(): void {
    if (!this.item?.itemId) return;
    this.pageRequests.add(
      this.jewellery.deleteTryOn(this.item.itemId).subscribe({
        next: () => {
          if (this.tryOnResult) URL.revokeObjectURL(this.tryOnResult);
          this.tryOnResult = '';
        },
        error: () => {
          this.tryOnError =
            'Could not remove the saved preview. Please try again.';
        },
      }),
    );
  }

  private loadSavedTryOn(id: number): void {
    this.pageRequests.add(
      this.jewellery.savedTryOn(id).subscribe({
        next: (image) => this.setTryOnResult(image),
        error: (response) => {
          if (response.status === 401) this.requireTryOnLogin();
        },
      }),
    );
  }

  private requireTryOnLogin(): void {
    this.tryOnNeedsLogin = true;
    this.tryOnError =
      'Your sign-in is no longer valid. Sign in again to create a preview.';
  }

  private setTryOnResult(image: Blob): void {
    if (this.tryOnResult) URL.revokeObjectURL(this.tryOnResult);
    this.tryOnResultType = image.type;
    this.tryOnResult = URL.createObjectURL(image);
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
    this.pageRequests.unsubscribe();
    if (this.cartNoticeTimer) clearTimeout(this.cartNoticeTimer);
    if (this.tryOnPreview) URL.revokeObjectURL(this.tryOnPreview);
    if (this.tryOnResult) URL.revokeObjectURL(this.tryOnResult);
  }

  addToCart(): void {
    if (!this.item || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    try {
      this.cart.add(this.item, Number(this.form.controls.quantity.value));
      this.message = `${this.item.itemName} added to your cart.`;
      this.error = '';
      this.clearCartNoticeAfterDelay();
    } catch (error) {
      this.error = (error as Error).message;
      this.message = '';
      this.clearCartNoticeAfterDelay();
    }
  }
  private clearCartNoticeAfterDelay(): void {
    if (this.cartNoticeTimer) clearTimeout(this.cartNoticeTimer);
    this.cartNoticeTimer = setTimeout(() => {
      this.message = '';
      this.error = '';
    }, 2000);
  }
}
