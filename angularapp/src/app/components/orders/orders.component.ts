import { Component, OnInit } from '@angular/core';
import { JewelleryOrder } from '../../models/jewellery-order.model';
import { JewelleryService } from '../../services/jewellery.service';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute } from '@angular/router';
import { FeedbackService } from '../../services/feedback.service';
import { ConfirmationService } from '../../services/confirmation.service';

interface PurchaseGroup {
  key: string;
  label: string;
  orders: JewelleryOrder[];
  total: number;
  date?: string;
}

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css'],
})
export class OrdersComponent implements OnInit {
  groups: PurchaseGroup[] = [];
  loading = true;
  error = '';
  message = '';
  reviewedIds = new Set<number>();
  constructor(
    private jewellery: JewelleryService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private feedback: FeedbackService,
    private confirmation: ConfirmationService,
  ) {}
  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const checkoutId = params.get('placed');
      if (checkoutId)
        this.message = 'Payment confirmed. Your order is now in My Orders.';
      this.load();
    });
  }
  load(): void {
    this.loading = true;
    this.feedback.getAllfeedbacksByUserId(this.auth.getUserId()).subscribe({
      next: (entries) =>
        (this.reviewedIds = new Set(
          entries
            .map((entry) => entry.itemId)
            .filter((id): id is number => !!id),
        )),
      error: () => (this.reviewedIds = new Set<number>()),
    });
    this.jewellery.getOrdersByUserId(this.auth.getUserId()).subscribe({
      next: (orders) => {
        const groups = new Map<string, PurchaseGroup>();
        for (const order of orders) {
          const key = order.checkoutId
            ? `checkout-${order.checkoutId}`
            : `order-${order.orderId}`;
          let group = groups.get(key);
          if (!group) {
            group = {
              key,
              label: `Order #${order.orderId}`,
              orders: [],
              total: 0,
              date: order.orderDate,
            };
            groups.set(key, group);
          }
          group.orders.push(order);
          group.total += order.totalPrice || 0;
        }
        this.groups = Array.from(groups.values()).map((group) => ({
          ...group,
          label: `${group.orders.length === 1 ? 'Order' : 'Orders'} ${group.orders.map((order) => `#${order.orderId}`).join(', ')}`,
        }));
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load orders. Please try again.';
        this.loading = false;
      },
    });
  }
  async remove(order: JewelleryOrder): Promise<void> {
    if (!['Pending', 'Approved'].includes(order.status)) return;
    if (
      !order.orderId ||
      !(await this.confirmation.confirm(
        'Cancel this item?',
        `${order.item?.itemName} from order #${order.orderId} will be cancelled and stock released.${order.paymentStatus === 'Paid' ? ' A Razorpay refund will be initiated.' : ''} The record remains in your history.`,
        'Cancel item',
      ))
    )
      return;
    this.jewellery.deleteOrder(order.orderId).subscribe({
      next: () => {
        this.message =
          order.paymentStatus === 'Paid'
            ? 'Cancellation submitted and a Razorpay refund initiated. Stock has been released.'
            : 'Order cancelled and stock released. The record remains in your history.';
        this.load();
      },
      error: (e) =>
        (this.error = e.error?.message || 'Could not cancel order.'),
    });
  }
}
