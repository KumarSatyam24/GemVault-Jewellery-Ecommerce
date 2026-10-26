import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { JewelleryItem } from '../../models/jewellery-item.model';
import { JewelleryService } from '../../services/jewellery.service';
import { DropdownOption } from '../dropdown/dropdown.component';
import {
  handleProductImageError,
  PRODUCT_IMAGE_FALLBACK,
} from '../../config/product-image.config';
import { AuthService } from '../../services/auth.service';
import { WishlistService } from '../../services/wishlist.service';

@Component({
  selector: 'app-catalog',
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css'],
})
export class CatalogComponent implements OnInit {
  imageFallback = PRODUCT_IMAGE_FALLBACK;
  imageFailed = handleProductImageError;
  items: JewelleryItem[] = [];
  loading = true;
  search = '';
  category = 'All';
  sort = 'featured';
  sortOptions: DropdownOption[] = [
    { label: 'Featured', value: 'featured' },
    { label: 'Price: low to high', value: 'low' },
    { label: 'Price: high to low', value: 'high' },
    { label: 'Name', value: 'name' },
  ];
  error = '';
  recommendations: JewelleryItem[] = [];
  wishedIds = new Set<number>();
  searchHistory: string[] = [];
  searchFocused = false;
  private searchTimer?: number;
  private searchBlurTimer?: number;
  constructor(
    private jewellery: JewelleryService,
    private router: Router,
    public auth: AuthService,
    private wishlist: WishlistService,
  ) {}
  ngOnInit(): void {
    this.load();
    if (this.auth.isCustomer()) {
      this.wishlist
        .all()
        .subscribe(
          (items) =>
            (this.wishedIds = new Set(items.map((item) => item.itemId || 0))),
        );
      this.refreshRecommendations();
      this.wishlist
        .searchHistory()
        .subscribe((history) => (this.searchHistory = history));
    }
  }
  load(): void {
    this.loading = true;
    this.jewellery.getAllItems().subscribe({
      next: (items) => {
        this.items = items.filter((item) => item.active !== false);
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load the collection. Please try again.';
        this.loading = false;
      },
    });
  }
  get categories(): string[] {
    return ['All', ...new Set(this.items.map((item) => item.category))];
  }
  get displayedItems(): JewelleryItem[] {
    const query = this.search.trim().toLowerCase();
    const result = this.items.filter(
      (item) =>
        (this.category === 'All' || item.category === this.category) &&
        (!query ||
          `${item.itemName} ${item.category} ${item.material}`
            .toLowerCase()
            .includes(query)),
    );
    if (this.sort === 'low')
      result.sort((a, b) => (a.totalPrice || 0) - (b.totalPrice || 0));
    if (this.sort === 'high')
      result.sort((a, b) => (b.totalPrice || 0) - (a.totalPrice || 0));
    if (this.sort === 'name')
      result.sort((a, b) => a.itemName.localeCompare(b.itemName));
    return result;
  }
  select(item: JewelleryItem): void {
    this.router.navigate(['/products', item.itemId]);
  }
  toggleWishlist(item: JewelleryItem): void {
    if (!item.itemId) return;
    if (!this.auth.isCustomer()) {
      this.router.navigate(['/login']);
      return;
    }
    const wasSaved = this.wishedIds.has(item.itemId);
    const done = () => {
      if (wasSaved) this.wishedIds.delete(item.itemId!);
      else this.wishedIds.add(item.itemId!);
      this.wishedIds = new Set(this.wishedIds);
      this.refreshRecommendations();
    };
    if (wasSaved) this.wishlist.remove(item.itemId).subscribe(done);
    else this.wishlist.add(item.itemId).subscribe(done);
  }
  get searchSuggestions(): string[] {
    const query = this.search.trim().toLowerCase();
    return this.searchHistory
      .filter((term) => !query || term.toLowerCase().includes(query))
      .slice(0, 6);
  }
  showSearchHistory(): void {
    window.clearTimeout(this.searchBlurTimer);
    this.searchFocused = true;
  }
  hideSearchHistory(): void {
    this.searchBlurTimer = window.setTimeout(
      () => (this.searchFocused = false),
      160,
    );
  }
  selectSearch(term: string): void {
    this.search = term;
    this.searchFocused = false;
    this.recordSearch(term);
  }
  recordSearch(term = this.search.trim()): void {
    window.clearTimeout(this.searchTimer);
    if (!this.auth.isCustomer() || term.length < 2) return;
    this.searchTimer = window.setTimeout(
      () =>
        this.wishlist.recordSearch(term).subscribe({
          next: () => {
            this.searchHistory = [
              term,
              ...this.searchHistory.filter(
                (entry) => entry.toLowerCase() !== term.toLowerCase(),
              ),
            ].slice(0, 10);
            this.refreshRecommendations();
          },
        }),
      700,
    );
  }
  private refreshRecommendations(): void {
    this.wishlist
      .recommendations()
      .subscribe((items) => (this.recommendations = items));
  }
}
