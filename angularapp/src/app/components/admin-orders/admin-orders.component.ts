import { Component, OnInit } from '@angular/core';
import { JewelleryOrder } from '../../models/jewellery-order.model';
import { JewelleryService } from '../../services/jewellery.service';
import { DropdownOption } from '../dropdown/dropdown.component';
@Component({
  selector: 'app-admin-orders',
  templateUrl: './admin-orders.component.html',
  styleUrls: ['./admin-orders.component.css'],
})
export class AdminOrdersComponent implements OnInit {
  orders: JewelleryOrder[] = [];
  loading = true;
  filter = 'All';
  filterOptions: DropdownOption[] = [
    'All',
    'Pending',
    'Approved',
    'Delivered',
    'Rejected',
    'Cancelled',
  ].map((value) => ({ label: value, value }));
  get visibleOrders(): JewelleryOrder[] {
    return this.filter === 'All'
      ? this.orders
      : this.orders.filter((order) => order.status === this.filter);
  }
  get pendingCount(): number {
    return this.orders.filter((order) => order.status === 'Pending').length;
  }
  error = '';
  message = '';
  cancelReasons: Record<number, string> = {};
  constructor(private jewellery: JewelleryService) {}
  ngOnInit(): void {
    this.load();
  }
  load(): void {
    this.loading = true;
    this.jewellery.getAllOrders().subscribe({
      next: (x) => {
        this.orders = [...x].sort(
          (a, b) => (b.orderId || 0) - (a.orderId || 0),
        );
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load orders.';
        this.loading = false;
      },
    });
  }
  setStatus(order: JewelleryOrder, status: string): void {
    if (!order.orderId) return;
    this.error = '';
    this.jewellery.updateOrder(order.orderId, { status }).subscribe({
      next: () => {
        this.message = 'Order updated.';
        this.load();
      },
      error: (e) =>
        (this.error = e.error?.message || 'Could not update order.'),
    });
  }
  cancel(order: JewelleryOrder): void {
    if (!order.orderId) return;
    const reason = this.cancelReasons[order.orderId]?.trim();
    if (!reason || reason.length > 500) {
      this.error = 'Enter a cancellation reason of 1–500 characters.';
      return;
    }
    this.jewellery.cancelOrder(order.orderId, reason).subscribe({
      next: () => {
        this.message =
          order.paymentStatus === 'Paid'
            ? 'Booking cancelled and a Razorpay refund initiated.'
            : 'Booking cancelled and stock released.';
        this.error = '';
        this.load();
      },
      error: (e) =>
        (this.error = e.error?.message || 'Could not cancel booking.'),
    });
  }
}
