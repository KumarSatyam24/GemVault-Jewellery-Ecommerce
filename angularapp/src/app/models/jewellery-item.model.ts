export interface JewelleryItem {
  itemId?: number;
  itemName: string;
  sku?: string;
  active?: boolean;
  description: string;
  ratingAverage?: number | null;
  ratingCount?: number;
  imageUrl: string;
  category: string;
  material: string;
  purity: string;
  stoneClarity?: string;
  weightInGrams: number;
  stoneWeightInGrams?: number;
  makingCharges: number;
  pricePerGram: number;
  stoneValue?: number;
  vaCharges?: number;
  discountAmount?: number;
  cgstRate?: number;
  sgstRate?: number;
  roundOff?: number;
  totalPrice?: number;
  billing?: BillingBreakdown;
  stockQuantity: number;
  reorderThreshold: number;
}

export interface ProductReview {
  reviewerName: string;
  feedbackText: string;
  rating: number | null;
  date: string | null;
}

export interface BillingBreakdown {
  grossWeight: number;
  stoneWeight: number;
  netMetalWeight: number;
  metalRate: number;
  metalValue: number;
  stoneValue: number;
  makingCharges: number;
  vaCharges: number;
  discountAmount: number;
  taxableAmount: number;
  cgstRate: number;
  cgstAmount: number;
  sgstRate: number;
  sgstAmount: number;
  roundOff: number;
  grandTotal: number;
}

export interface MetalRateQuote {
  material: string;
  purity: string;
  ratePerGram: number;
  source: string;
  fetchedAt: string;
}
