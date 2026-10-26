import { BillingBreakdown } from './jewellery-item.model';

export interface CartLine {
  itemId: number;
  itemName: string;
  unitPrice: number;
  quantity: number;
  stockQuantity: number;
  billing?: BillingBreakdown;
}

export interface CheckoutResult {
  checkoutId: number;
  totalPrice: number;
}

export interface PaymentStart {
  checkoutId: number;
  razorpayOrderId: string;
  keyId: string;
  amountPaise: number;
  currency: string;
  buyerName: string;
  buyerEmail: string;
  buyerMobile: string;
}

export interface PaymentStatus {
  checkoutId: number;
  paymentStatus: 'Pending' | 'Paid' | 'Expired' | 'Refunded';
  order: CheckoutResult | null;
  payment: PaymentStart | null;
}
