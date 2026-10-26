import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  JewelleryItem,
  MetalRateQuote,
  ProductReview,
} from '../models/jewellery-item.model';
import { JewelleryOrder } from '../models/jewellery-order.model';
import { PaymentStart, PaymentStatus } from '../models/cart.model';
import { API_BASE_URL } from '../config/api.config';
@Injectable({ providedIn: 'root' })
export class JewelleryService {
  public apiUrl = API_BASE_URL;
  constructor(private http: HttpClient) {}
  getAllItems(): Observable<JewelleryItem[]> {
    return this.http.get<JewelleryItem[]>(this.apiUrl + '/jewellery-item');
  }
  getItemById(id: number): Observable<JewelleryItem> {
    return this.http.get<JewelleryItem>(this.apiUrl + '/jewellery-item/' + id);
  }
  getProductReviews(id: number): Observable<ProductReview[]> {
    return this.http.get<ProductReview[]>(
      `${this.apiUrl}/jewellery-item/${id}/reviews`,
    );
  }
  getSimilarItems(id: number): Observable<JewelleryItem[]> {
    return this.http.get<JewelleryItem[]>(
      `${this.apiUrl}/jewellery-item/${id}/similar`,
    );
  }
  savedTryOn(id: number): Observable<Blob> {
    return this.http.get(this.apiUrl + '/try-on/' + id, {
      responseType: 'blob',
    });
  }
  generateTryOn(id: number, photo: File): Observable<Blob> {
    const body = new FormData();
    body.append('photo', photo);
    return this.http.post(this.apiUrl + '/try-on/' + id, body, {
      responseType: 'blob',
    });
  }
  deleteTryOn(id: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + '/try-on/' + id);
  }
  getLowStockItems(): Observable<JewelleryItem[]> {
    return this.http.get<JewelleryItem[]>(
      this.apiUrl + '/jewellery-item/low-stock',
    );
  }
  getLiveMetalRate(
    material: string,
    purity: string,
    refresh = false,
  ): Observable<MetalRateQuote> {
    return this.http.get<MetalRateQuote>(this.apiUrl + '/metal-rates', {
      params: { material, purity, refresh },
    });
  }
  importCsv(
    file: File,
  ): Observable<{ created: number; updated: number; errors: string[] }> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<{
      created: number;
      updated: number;
      errors: string[];
    }>(this.apiUrl + '/jewellery-item/import', data);
  }
  csvTemplate(): Observable<Blob> {
    return this.http.get(this.apiUrl + '/jewellery-item/import-template', {
      responseType: 'blob',
    });
  }
  exportCsv(): Observable<Blob> {
    return this.http.get(this.apiUrl + '/jewellery-item/export', {
      responseType: 'blob',
    });
  }
  addItem(item: JewelleryItem): Observable<JewelleryItem> {
    return this.http.post<JewelleryItem>(this.apiUrl + '/jewellery-item', item);
  }
  updateItem(id: number, item: JewelleryItem): Observable<JewelleryItem> {
    return this.http.put<JewelleryItem>(
      this.apiUrl + '/jewellery-item/' + id,
      item,
    );
  }
  deleteItem(id: number): Observable<JewelleryItem> {
    return this.http.delete<JewelleryItem>(
      this.apiUrl + '/jewellery-item/' + id,
    );
  }
  archiveItem(id: number): Observable<JewelleryItem> {
    return this.http.post<JewelleryItem>(
      this.apiUrl + '/jewellery-item/' + id + '/archive',
      {},
    );
  }
  restoreItem(id: number): Observable<JewelleryItem> {
    return this.http.post<JewelleryItem>(
      this.apiUrl + '/jewellery-item/' + id + '/restore',
      {},
    );
  }
  restockItem(id: number, quantity: number): Observable<JewelleryItem> {
    return this.http.post<JewelleryItem>(
      this.apiUrl + '/jewellery-item/' + id + '/restock',
      { quantity },
    );
  }
  getAllOrders(): Observable<JewelleryOrder[]> {
    return this.http.get<JewelleryOrder[]>(this.apiUrl + '/jewellery-order');
  }
  getOrdersByUserId(id: number): Observable<JewelleryOrder[]> {
    return this.http.get<JewelleryOrder[]>(
      this.apiUrl + '/jewellery-order/user/' + id,
    );
  }
  addOrder(order: JewelleryOrder): Observable<JewelleryOrder> {
    return this.http.post<JewelleryOrder>(
      this.apiUrl + '/jewellery-order',
      order,
    );
  }
  updateOrder(
    id: number,
    order: Partial<JewelleryOrder>,
  ): Observable<JewelleryOrder> {
    return this.http.put<JewelleryOrder>(
      this.apiUrl + '/jewellery-order/' + id,
      order,
    );
  }
  deleteOrder(id: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + '/jewellery-order/' + id);
  }
  cancelOrder(id: number, reason: string): Observable<JewelleryOrder> {
    return this.http.post<JewelleryOrder>(
      this.apiUrl + '/jewellery-order/' + id + '/cancel',
      { reason },
    );
  }
  checkout(
    addressId: number,
    items: { itemId: number; quantity: number }[],
    comments: string,
  ): Observable<PaymentStart> {
    return this.http.post<PaymentStart>(this.apiUrl + '/checkout', {
      addressId,
      items,
      comments,
    });
  }

  verifyCheckout(
    checkoutId: number,
    razorpayOrderId: string,
    razorpayPaymentId: string,
    razorpaySignature: string,
  ): Observable<PaymentStatus> {
    return this.http.post<PaymentStatus>(this.apiUrl + '/checkout/verify', {
      checkoutId,
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
    });
  }

  checkoutStatus(checkoutId: number): Observable<PaymentStatus> {
    return this.http.get<PaymentStatus>(
      this.apiUrl + '/checkout/' + checkoutId + '/status',
    );
  }
}
