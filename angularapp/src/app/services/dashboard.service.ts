import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../config/api.config';
import { DashboardOverview } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private http: HttpClient) {}
  overview(from: string, to: string) {
    return this.http.get<DashboardOverview>(`${API_BASE_URL}/admin/dashboard`, {
      params: { from, to },
    });
  }
}
