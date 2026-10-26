export const PRODUCT_IMAGE_FALLBACK = 'assets/images/product-placeholder.svg';

export function handleProductImageError(event: Event): void {
  const image = event.target as HTMLImageElement;
  if (!image.src.includes('product-placeholder.svg')) image.src = PRODUCT_IMAGE_FALLBACK;
}
