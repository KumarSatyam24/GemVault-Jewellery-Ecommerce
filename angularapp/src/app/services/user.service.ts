import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { User } from '../models/user.model';
import { SavedAddress } from '../models/saved-address.model';
import { API_BASE_URL } from '../config/api.config';
@Injectable({ providedIn: 'root' })
export class UserService {
  private addressChanges = new Subject<void>();
  addressChanges$ = this.addressChanges.asObservable();
  public apiUrl = API_BASE_URL;
  constructor(private http: HttpClient) {}
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl + '/users');
  }
  register(user: User): Observable<User> {
    return this.http.post<User>(this.apiUrl + '/register', user);
  }
  inviteBuyer(buyer: {
    username: string;
    email: string;
    mobileNumber: string;
  }): Observable<User> {
    return this.http.post<User>(this.apiUrl + '/admin/buyers/invite', buyer);
  }
  getProfile(): Observable<User> {
    return this.http.get<User>(this.apiUrl + '/profile');
  }
  updateProfile(profile: {
    username: string;
    mobileNumber: string;
  }): Observable<User> {
    return this.http.put<User>(this.apiUrl + '/profile', profile);
  }
  getAddresses(): Observable<SavedAddress[]> {
    return this.http.get<SavedAddress[]>(this.apiUrl + '/profile/addresses');
  }
  addAddress(address: SavedAddress): Observable<SavedAddress> {
    return this.http
      .post<SavedAddress>(this.apiUrl + '/profile/addresses', address)
      .pipe(tap(() => this.addressChanges.next()));
  }
  updateAddress(id: number, address: SavedAddress): Observable<SavedAddress> {
    return this.http
      .put<SavedAddress>(this.apiUrl + '/profile/addresses/' + id, address)
      .pipe(tap(() => this.addressChanges.next()));
  }
  deleteAddress(id: number): Observable<void> {
    return this.http
      .delete<void>(this.apiUrl + '/profile/addresses/' + id)
      .pipe(tap(() => this.addressChanges.next()));
  }
}
