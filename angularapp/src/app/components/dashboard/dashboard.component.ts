import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { DashboardOverview } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';
import { JewelleryService } from '../../services/jewellery.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit, OnDestroy {
  data?: DashboardOverview;
  loading = true;
  error = '';
  validationError = '';
  range = '30';
  metric: 'sales' | 'orders' = 'sales';
  today = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Kolkata',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  draftFrom = this.shiftDate(this.today, -29);
  draftTo = this.today;
  private request?: Subscription;
  private stockRequest?: Subscription;
  stockAlerts: DashboardOverview['stockAlerts'] = [];
  stockPage = 1;
  readonly stockPageSize = 6;
  stockLoading = true;
  stockError = '';
  private appliedFrom = this.draftFrom;
  private appliedTo = this.draftTo;
  readonly rangeOptions = [
    { label: 'Last 7 days', value: '7' },
    { label: 'Last 30 days', value: '30' },
    { label: 'Last 90 days', value: '90' },
    { label: 'Last 365 days', value: '365' },
    { label: 'Custom dates', value: 'custom' },
  ];
  readonly statusNames: Record<string, string> = {
    AwaitingPayment: 'Awaiting payment',
  };
  readonly statusColors: Record<string, string> = {
    Pending: '#b48b42',
    Approved: '#8e9b89',
    Delivered: '#526d59',
    AwaitingPayment: '#b9b0a3',
    Cancelled: '#a77b6d',
    Rejected: '#68645e',
    Unknown: '#96918b',
  };
  trendLabels: string[] = [];
  trendSales: number[] = [];
  trendOrders: number[] = [];
  statusLabels: string[] = [];
  statusValues: number[] = [];
  statusPalette: string[] = [];
  categoryLabels: string[] = [];
  categoryValues: number[] = [];

  constructor(
    private dashboard: DashboardService,
    private jewellery: JewelleryService,
  ) {}
  ngOnInit(): void {
    this.refresh();
  }
  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.stockRequest?.unsubscribe();
  }

  get stockPageCount(): number {
    return Math.max(1, Math.ceil(this.stockAlerts.length / this.stockPageSize));
  }

  get visibleStockAlerts(): DashboardOverview['stockAlerts'] {
    const start = (this.stockPage - 1) * this.stockPageSize;
    return this.stockAlerts.slice(start, start + this.stockPageSize);
  }

  selectRange(): void {
    this.validationError = '';
    if (this.range === 'custom') return;
    this.draftFrom = this.shiftDate(this.today, 1 - Number(this.range));
    this.draftTo = this.today;
    this.applyDates();
  }
  applyDates(): void {
    const days =
      (Date.parse(this.draftTo) - Date.parse(this.draftFrom)) / 86400000 + 1;
    if (
      !this.draftFrom ||
      !this.draftTo ||
      !Number.isFinite(days) ||
      days < 1 ||
      days > 366 ||
      this.draftTo > this.today
    ) {
      this.validationError = 'Choose 1 to 366 days, ending today or earlier.';
      return;
    }
    this.validationError = '';
    this.appliedFrom = this.draftFrom;
    this.appliedTo = this.draftTo;
    this.refresh();
  }
  refresh(): void {
    this.request?.unsubscribe();
    this.stockRequest?.unsubscribe();
    this.stockLoading = true;
    this.stockError = '';
    this.stockRequest = this.jewellery.getLowStockItems().subscribe({
      next: (items) => {
        this.stockAlerts = items
          .filter((item) => item.active === true)
          .sort(
            (a, b) =>
              a.stockQuantity - b.stockQuantity || a.itemId! - b.itemId!,
          )
          .map((item) => ({
            itemId: item.itemId!,
            itemName: item.itemName,
            sku: item.sku || '',
            stock: item.stockQuantity,
            threshold: item.reorderThreshold,
          }));
        this.stockPage = Math.min(this.stockPage, this.stockPageCount);
        this.stockLoading = false;
      },
      error: () => {
        this.stockLoading = false;
        this.stockError =
          'Stock watch could not be updated. Please refresh to try again.';
      },
    });
    this.loading = true;
    this.error = '';
    this.request = this.dashboard
      .overview(this.appliedFrom, this.appliedTo)
      .subscribe({
        next: (data) => {
          this.data = data;
          this.trendLabels = data.trend.map((point) =>
            new Intl.DateTimeFormat('en-IN', {
              timeZone: 'UTC',
              ...(data.granularity === 'month'
                ? { month: 'short', year: '2-digit' }
                : { day: 'numeric', month: 'short' }),
            }).format(new Date(`${point.date}T00:00:00Z`)),
          );
          this.trendSales = data.trend.map((point) => point.sales);
          this.trendOrders = data.trend.map((point) => point.orders);
          const statuses = data.statuses.filter((status) => status.count > 0);
          this.statusLabels = statuses.map((status) =>
            this.statusName(status.status),
          );
          this.statusValues = statuses.map((status) => status.count);
          this.statusPalette = statuses.map(
            (status) => this.statusColors[status.status] || '#96918b',
          );
          this.categoryLabels = data.categories.map(
            (category) => category.category,
          );
          this.categoryValues = data.categories.map(
            (category) => category.sales,
          );
          this.loading = false;
        },
        error: (response) => {
          this.loading = false;
          this.error =
            response.error?.message ||
            'The dashboard could not be loaded. Please try again.';
        },
      });
  }
  statusName(status: string): string {
    return this.statusNames[status] || status;
  }
  comparison(current: number, previous: number): string {
    if (!previous)
      return current
        ? 'No activity in the previous period'
        : 'No change from the previous period';
    const percent = ((current - previous) / previous) * 100;
    if (Math.abs(percent) < 0.05) return 'No change from the previous period';
    return `${percent > 0 ? '+' : ''}${percent.toFixed(1)}% vs previous period`;
  }
  private shiftDate(date: string, days: number): string {
    const value = new Date(`${date}T00:00:00Z`);
    value.setUTCDate(value.getUTCDate() + days);
    return value.toISOString().slice(0, 10);
  }
}
