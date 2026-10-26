import { BillingBreakdown } from './jewellery-item.model';

export interface JewelleryOrder {
  orderId?: number;
  checkoutId?: number;
  cancellationReason?: string;
  userId: number;
  itemId: number;
  quantity: number;
  orderDate?: string;
  status: string;
  paymentStatus?: string;
  refundStatus?: string;
  deliveryAddress?: string;
  comments?: string;
  totalPrice?: number;
  billing?: BillingBreakdown;
  invoiceItem?: {
    itemName: string;
    sku?: string;
    material?: string;
    purity?: string;
  };
  item?: {
    itemId: number;
    itemName: string;
    sku?: string;
    material?: string;
    purity?: string;
  };
  user?: { userId: number; username: string };
}
