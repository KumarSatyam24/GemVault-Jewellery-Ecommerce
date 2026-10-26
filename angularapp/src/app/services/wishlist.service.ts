import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { JewelleryItem } from '../models/jewellery-item.model';
@Injectable({ providedIn: 'root' })
export class WishlistService {
  constructor(private http: HttpClient) {}
  all(): Observable<JewelleryItem[]> {
    return this.http.get<JewelleryItem[]>(API_BASE_URL + '/wishlist');
  }
  add(itemId: number): Observable<JewelleryItem> {
    return this.http.post<JewelleryItem>(
      API_BASE_URL + '/wishlist/' + itemId,
      {},
    );
  }
  remove(itemId: number): Observable<void> {
    return this.http.delete<void>(API_BASE_URL + '/wishlist/' + itemId);
  }
  recommendations(): Observable<JewelleryItem[]> {
    return this.http.get<JewelleryItem[]>(API_BASE_URL + '/recommendations');
  }
  searchHistory(): Observable<string[]> {
    return this.http.get<string[]>(
      API_BASE_URL + '/recommendations/search-history',
    );
  }
  recordSearch(term: string): Observable<void> {
    return this.http.post<void>(
      API_BASE_URL + '/recommendations/search-history',
      { term },
    );
  }
}
