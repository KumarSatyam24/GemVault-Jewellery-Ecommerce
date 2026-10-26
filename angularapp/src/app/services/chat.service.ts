import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly url = `${API_BASE_URL}/chat`;

  constructor(private http: HttpClient) {}

  sendMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url, request);
  }

  history(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.url}/history/${sessionId}`);
  }

  clearMemory(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/memory/${sessionId}`);
  }
}
