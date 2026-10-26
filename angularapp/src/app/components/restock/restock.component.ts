import { Component, OnInit } from '@angular/core';
import { JewelleryItem } from '../../models/jewellery-item.model';
import { JewelleryService } from '../../services/jewellery.service';
@Component({
  selector: 'app-restock',
  templateUrl: './restock.component.html',
  styleUrls: ['./restock.component.css'],
})
export class RestockComponent implements OnInit {
  items: JewelleryItem[] = [];
  search = '';
  get filteredItems(): JewelleryItem[] {
    const query = this.search.trim().toLowerCase();
    return query
      ? this.items.filter((item) =>
          [item.itemName, item.sku, item.category, item.material]
            .some((value) => value?.toLowerCase().includes(query)),
        )
      : this.items;
  }
  error = '';
  message = '';
  quantities: Record<number, number> = {};
  savingId?: number;
  constructor(private jewellery: JewelleryService) {}
  ngOnInit(): void {
    this.load();
  }
  load(): void {
    this.jewellery.getLowStockItems().subscribe({
      next: (x) => {
        this.items = x;
        this.error = '';
      },
      error: () => (this.error = 'Could not load restock items.'),
    });
  }
  restock(item: JewelleryItem): void {
    const id = item.itemId;
    const quantity = id ? Number(this.quantities[id]) : 0;
    if (!id || !Number.isInteger(quantity) || quantity < 1) {
      this.error = 'Enter a whole number of pieces to add.';
      return;
    }
    this.savingId = id;
    this.error = '';
    this.jewellery.restockItem(id, quantity).subscribe({
      next: () => {
        this.savingId = undefined;
        this.message = `${quantity} added to ${item.itemName}.`;
        delete this.quantities[id];
        this.load();
      },
      error: (e) => {
        this.savingId = undefined;
        this.error = e.error?.message || 'Could not restock this piece.';
      },
    });
  }
}
