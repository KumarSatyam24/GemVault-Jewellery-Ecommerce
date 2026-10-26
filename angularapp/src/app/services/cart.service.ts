import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CartLine } from '../models/cart.model';
import { JewelleryItem } from '../models/jewellery-item.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private ownerId = 0;
  private linesSubject = new BehaviorSubject<CartLine[]>([]);
  lines$ = this.linesSubject.asObservable();

  constructor(private auth: AuthService) {}

  private syncOwner(): void {
    const id = this.auth.getUserId();
    if (id === this.ownerId) return;
    this.ownerId = id;
    try {
      this.linesSubject.next(
        JSON.parse(localStorage.getItem(this.key()) || '[]'),
      );
    } catch {
      this.linesSubject.next([]);
    }
  }

  private key(): string {
    return 'gemvault.cart.' + this.ownerId;
  }
  private save(lines: CartLine[]): void {
    localStorage.setItem(this.key(), JSON.stringify(lines));
    this.linesSubject.next(lines);
  }
  get lines(): CartLine[] {
    this.syncOwner();
    return this.linesSubject.value;
  }
  get quantityCount(): number {
    return this.lines.reduce((sum, line) => sum + line.quantity, 0);
  }
  get total(): number {
    return this.lines.reduce(
      (sum, line) => sum + line.unitPrice * line.quantity,
      0,
    );
  }
  get billingTotal() {
    const fields = [
      'metalValue',
      'stoneValue',
      'makingCharges',
      'vaCharges',
      'discountAmount',
      'taxableAmount',
      'cgstAmount',
      'sgstAmount',
      'roundOff',
      'grandTotal',
    ] as const;
    return fields.reduce(
      (totals, field) => {
        totals[field] = this.lines.reduce(
          (sum, line) => sum + (line.billing?.[field] || 0) * line.quantity,
          0,
        );
        return totals;
      },
      {} as Record<(typeof fields)[number], number>,
    );
  }

  add(item: JewelleryItem, quantity: number): void {
    this.syncOwner();
    if (
      !item.itemId ||
      !Number.isInteger(quantity) ||
      quantity < 1 ||
      item.active === false
    )
      throw new Error('Select an available item and a whole-number quantity.');
    const lines = [...this.lines];
    const existing = lines.find((line) => line.itemId === item.itemId);
    const nextQuantity = (existing?.quantity || 0) + quantity;
    if (nextQuantity > item.stockQuantity)
      throw new Error('Only ' + item.stockQuantity + ' in stock.');
    if (existing) {
      existing.quantity = nextQuantity;
      existing.stockQuantity = item.stockQuantity;
      existing.unitPrice = item.totalPrice || 0;
      existing.billing = item.billing;
    } else {
      lines.push({
        itemId: item.itemId,
        itemName: item.itemName,
        unitPrice: item.totalPrice || 0,
        quantity,
        stockQuantity: item.stockQuantity,
        billing: item.billing,
      });
    }
    this.save(lines);
  }

  change(itemId: number, quantity: number): void {
    this.syncOwner();
    const line = this.lines.find((item) => item.itemId === itemId);
    if (!line) return;
    if (
      !Number.isInteger(quantity) ||
      quantity < 1 ||
      quantity > line.stockQuantity
    )
      throw new Error('Enter 1 to ' + line.stockQuantity + '.');
    this.save(
      this.lines.map((item) =>
        item.itemId === itemId ? { ...item, quantity } : item,
      ),
    );
  }
  remove(itemId: number): void {
    this.syncOwner();
    this.save(this.lines.filter((item) => item.itemId !== itemId));
  }
  clear(): void {
    this.syncOwner();
    this.save([]);
  }

  reconcile(items: JewelleryItem[]): boolean {
    const current = this.lines;
    const available = new Map(
      items.filter((item) => item.itemId).map((item) => [item.itemId, item]),
    );
    const refreshed = current.reduce<CartLine[]>((result, line) => {
      const item = available.get(line.itemId);
      if (
        item &&
        item.active !== false &&
        item.stockQuantity > 0 &&
        Number.isInteger(line.quantity) &&
        line.quantity > 0
      ) {
        result.push({
          ...line,
          itemName: item.itemName,
          unitPrice: item.totalPrice || 0,
          billing: item.billing,
          stockQuantity: item.stockQuantity,
          quantity: Math.min(line.quantity, item.stockQuantity),
        });
      }
      return result;
    }, []);
    const changed = JSON.stringify(current) !== JSON.stringify(refreshed);
    if (changed) this.save(refreshed);
    return changed;
  }
}
