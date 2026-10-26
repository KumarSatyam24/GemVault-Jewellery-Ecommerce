import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  NgZone,
} from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { SavedAddress } from '../../models/saved-address.model';
import { CartService } from '../../services/cart.service';
import { JewelleryService } from '../../services/jewellery.service';
import { UserService } from '../../services/user.service';
import { DropdownOption } from '../dropdown/dropdown.component';
import { PaymentStart, PaymentStatus } from '../../models/cart.model';
import { AuthService } from '../../services/auth.service';

interface RazorpayResponse {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => {
      open(): void;
      on(event: string, handler: () => void): void;
    };
  }
}

@Component({
  selector: 'app-cart-sidebar',
  templateUrl: './cart-sidebar.component.html',
  styleUrls: ['./cart-sidebar.component.css'],
})
export class CartSidebarComponent implements OnInit, OnChanges, OnDestroy {
  @Input() mobileOpen = false;
  @Output() closeRequested = new EventEmitter<void>();
  addresses: SavedAddress[] = [];
  addressId = 0;
  get addressOptions(): DropdownOption[] {
    return [
      { label: 'Select an address', value: 0 },
      ...this.addresses.map((address) => ({
        label: `${address.label} — ${address.city}`,
        value: address.addressId || 0,
      })),
    ];
  }
  comments = '';
  error = '';
  saving = false;
  refreshing = false;
  pendingPayment: PaymentStart | null = null;
  private navigation?: Subscription;
  private addressUpdates?: Subscription;

  constructor(
    public cart: CartService,
    private users: UserService,
    private jewellery: JewelleryService,
    private router: Router,
    private auth: AuthService,
    private zone: NgZone,
  ) {}

  ngOnInit(): void {
    this.cart.lines;
    this.loadAddresses();
    this.restorePayment();
    this.navigation = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.loadAddresses());
    this.addressUpdates = this.users.addressChanges$.subscribe(() =>
      this.loadAddresses(),
    );
  }
  ngOnChanges(): void {
    if (this.mobileOpen) {
      this.loadAddresses();
      this.refreshCart();
    }
  }
  refreshCart(continueCheckout = false): void {
    if (!this.cart.lines.length) return;
    this.refreshing = true;
    this.jewellery.getAllItems().subscribe({
      next: (items) => {
        this.refreshing = false;
        const changed = this.cart.reconcile(items);
        if (changed) {
          this.error =
            'Your cart was updated for current prices and stock. Please review it before placing the order.';
          this.saving = false;
        } else if (continueCheckout) {
          this.placeOrder();
        }
      },
      error: () => {
        this.refreshing = false;
        this.saving = false;
        this.error =
          'Could not verify current prices and stock. Please try again.';
      },
    });
  }
  ngOnDestroy(): void {
    this.navigation?.unsubscribe();
    this.addressUpdates?.unsubscribe();
  }
  loadAddresses(): void {
    this.users.getAddresses().subscribe({
      next: (addresses) => {
        this.addresses = addresses;
        if (!addresses.some((address) => address.addressId === this.addressId))
          this.addressId = addresses[0]?.addressId || 0;
      },
      error: () => (this.error = 'Could not load saved addresses.'),
    });
  }
  change(itemId: number, value: string): void {
    try {
      this.cart.change(itemId, Number(value));
      this.error = '';
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
  checkout(): void {
    if (!this.cart.lines.length || !this.addressId) {
      this.error = 'Add items and select a saved address.';
      return;
    }
    this.saving = true;
    this.error = '';
    if (this.pendingPayment && this.savedCartMatches()) {
      this.openPayment(this.pendingPayment);
      return;
    }
    this.refreshCart(true);
  }
  private placeOrder(): void {
    const items = this.cart.lines.map(({ itemId, quantity }) => ({
      itemId,
      quantity,
    }));
    this.jewellery.checkout(this.addressId, items, this.comments).subscribe({
      next: (result) => {
        this.pendingPayment = result;
        sessionStorage.setItem(this.pendingKey(), String(result.checkoutId));
        sessionStorage.setItem(
          this.pendingKey() + ':cart',
          this.cartFingerprint(),
        );
        this.openPayment(result);
      },
      error: (error) => {
        this.error =
          error.error?.message ||
          'Could not place order. Check stock and try again.';
        this.saving = false;
      },
    });
  }

  private pendingKey(): string {
    return 'gemvault-razorpay-pending-' + this.auth.getUserId();
  }

  private cartFingerprint(): string {
    return JSON.stringify(
      this.cart.lines.map((line) => [line.itemId, line.quantity]),
    );
  }

  savedCartMatches(): boolean {
    return (
      sessionStorage.getItem(this.pendingKey() + ':cart') ===
      this.cartFingerprint()
    );
  }

  private restorePayment(): void {
    const id = Number(sessionStorage.getItem(this.pendingKey()));
    if (!id) return;
    this.jewellery.checkoutStatus(id).subscribe({
      next: (status) => {
        if (status.paymentStatus === 'Paid') this.finishPayment(status);
        else if (status.paymentStatus === 'Pending')
          this.pendingPayment = status.payment;
        else this.forgetPayment();
      },
      error: () => this.forgetPayment(),
    });
  }

  private loadRazorpay(): Promise<void> {
    if (window.Razorpay) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>(
        'script[data-razorpay-checkout]',
      );
      if (existing) existing.remove();
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.dataset['razorpayCheckout'] = 'true';
      script.onload = () => resolve();
      script.onerror = () =>
        reject(new Error('Could not load Razorpay checkout.'));
      document.head.appendChild(script);
    });
  }

  private async openPayment(payment: PaymentStart): Promise<void> {
    try {
      await this.loadRazorpay();
      this.zone.run(() => {
        const Checkout = window.Razorpay;
        if (!Checkout) throw new Error('Razorpay checkout is unavailable.');
        const gateway = new Checkout({
          key: payment.keyId,
          amount: payment.amountPaise,
          currency: payment.currency,
          name: 'GemVault',
          description: 'Fine jewellery',
          order_id: payment.razorpayOrderId,
          prefill: {
            name: payment.buyerName,
            email: payment.buyerEmail,
            contact: payment.buyerMobile,
          },
          theme: { color: '#b68b45' },
          handler: (response: RazorpayResponse) =>
            this.zone.run(() => this.verifyPayment(payment, response)),
          modal: {
            ondismiss: () =>
              this.zone.run(() => this.checkPayment(payment.checkoutId)),
          },
        });
        gateway.on('payment.failed', () =>
          this.zone.run(() => {
            this.error = 'Payment was not completed. You can try again.';
            this.saving = false;
          }),
        );
        this.saving = false;
        gateway.open();
      });
    } catch (error) {
      this.zone.run(() => {
        this.error = (error as Error).message;
        this.saving = false;
      });
    }
  }

  private verifyPayment(
    payment: PaymentStart,
    response: RazorpayResponse,
  ): void {
    this.saving = true;
    this.jewellery
      .verifyCheckout(
        payment.checkoutId,
        response.razorpay_order_id,
        response.razorpay_payment_id,
        response.razorpay_signature,
      )
      .subscribe({
        next: (status) => this.finishPayment(status),
        error: (error) => {
          this.error =
            error.error?.message ||
            'Payment confirmation is pending. Checking its status.';
          this.checkPayment(payment.checkoutId);
        },
      });
  }

  private checkPayment(checkoutId: number): void {
    this.jewellery.checkoutStatus(checkoutId).subscribe({
      next: (status) => {
        if (status.paymentStatus === 'Paid') this.finishPayment(status);
        else {
          if (status.paymentStatus === 'Expired') this.forgetPayment();
          this.error =
            status.paymentStatus === 'Expired'
              ? 'This payment session expired. Please check out again.'
              : 'Payment is not confirmed yet. Your cart is still here; you can retry.';
          this.saving = false;
        }
      },
      error: () => {
        this.error =
          'Could not confirm payment yet. Please retry or check My Orders.';
        this.saving = false;
      },
    });
  }

  private finishPayment(status: PaymentStatus): void {
    if (status.paymentStatus !== 'Paid') return;
    if (this.savedCartMatches()) this.cart.clear();
    this.forgetPayment();
    this.comments = '';
    this.saving = false;
    this.closeRequested.emit();
    this.router.navigate(['/orders'], {
      queryParams: { placed: status.checkoutId },
    });
  }

  private forgetPayment(): void {
    sessionStorage.removeItem(this.pendingKey());
    sessionStorage.removeItem(this.pendingKey() + ':cart');
    this.pendingPayment = null;
  }
}
