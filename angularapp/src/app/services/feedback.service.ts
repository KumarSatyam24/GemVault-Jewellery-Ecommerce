import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Feedback } from '../models/feedback.model';
import { API_BASE_URL } from '../config/api.config';
@Injectable({ providedIn: 'root' })
export class FeedbackService {
  public apiUrl = API_BASE_URL;
  constructor(private http: HttpClient) {}
  sendFeedback(feedback: Feedback): Observable<Feedback> {
    return this.http.post<Feedback>(this.apiUrl + '/feedback', feedback);
  }
  getAllfeedbacksByUserId(id: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(this.apiUrl + '/feedback/user/' + id);
  }
  getFeedbacksByItemId(id: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(this.apiUrl + '/feedback/item/' + id);
  }
  deleteFeedback(id: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + '/feedback/' + id);
  }
  getFeedbacks(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(this.apiUrl + '/feedback');
  }
}
