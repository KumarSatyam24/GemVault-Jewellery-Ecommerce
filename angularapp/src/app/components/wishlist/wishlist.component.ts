import { Component, OnInit } from '@angular/core';
import { JewelleryItem } from '../../models/jewellery-item.model';
import { WishlistService } from '../../services/wishlist.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-wishlist',
  templateUrl: './wishlist.component.html',
})
export class WishlistComponent implements OnInit {
  items: JewelleryItem[] = [];
  loading = true;
  error = '';
  constructor(
    private wishlist: WishlistService,
    private router: Router,
  ) {}
  ngOnInit(): void {
    this.load();
  }
  load(): void {
    this.wishlist.all().subscribe({
      next: (items) => {
        this.items = items;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load your wishlist.';
        this.loading = false;
      },
    });
  }
  remove(item: JewelleryItem): void {
    if (!item.itemId) return;
    this.wishlist
      .remove(item.itemId)
      .subscribe(
        () => (this.items = this.items.filter((x) => x.itemId !== item.itemId)),
      );
  }
  view(item: JewelleryItem): void {
    this.router.navigate(['/products', item.itemId], {
      queryParams: { from: 'wishlist' },
    });
  }
}
