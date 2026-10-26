import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import type { Chart, ChartOptions } from 'chart.js';

@Component({
  selector: 'app-dashboard-chart',
  templateUrl: './dashboard-chart.component.html',
  host: { class: 'relative block h-full min-w-0' },
})
export class DashboardChartComponent
  implements AfterViewInit, OnChanges, OnDestroy
{
  @ViewChild('canvas', { static: true }) canvas!: ElementRef<HTMLCanvasElement>;
  @Input() type: 'line' | 'bar' | 'doughnut' = 'line';
  @Input() labels: string[] = [];
  @Input() values: number[] = [];
  @Input() colors: string[] = [];
  @Input() currency = false;
  @Input() horizontal = false;
  @Input() label = '';
  private chart?: Chart;
  private ready = false;
  private destroyed = false;
  private chartConstructor?: typeof Chart;
  private observer?: MutationObserver;

  async ngAfterViewInit(): Promise<void> {
    const charts = await import('chart.js');
    if (this.destroyed) return;
    charts.Chart.register(
      charts.LineController,
      charts.LineElement,
      charts.PointElement,
      charts.BarController,
      charts.BarElement,
      charts.DoughnutController,
      charts.ArcElement,
      charts.CategoryScale,
      charts.LinearScale,
      charts.Tooltip,
      charts.Filler,
    );
    this.chartConstructor = charts.Chart;
    this.ready = true;
    this.render();
    this.observer = new MutationObserver(() => this.render());
    this.observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class', 'data-theme'],
    });
  }
  ngOnChanges(): void {
    if (this.ready) this.render();
  }
  ngOnDestroy(): void {
    this.destroyed = true;
    this.chart?.destroy();
    this.observer?.disconnect();
  }

  private render(): void {
    if (!this.chartConstructor || this.destroyed) return;
    this.chart?.destroy();
    const dark = document.documentElement.classList.contains('dark');
    const muted = dark ? '#bdb4aa' : '#6c6863';
    const grid = dark ? 'rgba(255,255,255,.09)' : 'rgba(26,26,26,.07)';
    const reduced = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;
    const format = (value: number) =>
      new Intl.NumberFormat(
        'en-IN',
        this.currency
          ? { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }
          : { maximumFractionDigits: 0 },
      ).format(value);
    const compact = (value: number) => {
      const divisor =
        value >= 10000000
          ? 10000000
          : value >= 100000
            ? 100000
            : value >= 1000
              ? 1000
              : 1;
      const suffix =
        divisor === 10000000
          ? 'Cr'
          : divisor === 100000
            ? 'L'
            : divisor === 1000
              ? 'k'
              : '';
      return `${this.currency ? '₹' : ''}${Number((value / divisor).toFixed(1))}${suffix}`;
    };
    const options: ChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      animation: reduced ? false : { duration: 350 },
      indexAxis: this.horizontal ? 'y' : 'x',
      interaction: {
        intersect: false,
        mode: this.type === 'line' ? 'index' : 'nearest',
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: dark ? '#f9f8f6' : '#1a1a1a',
          titleColor: dark ? '#1a1a1a' : '#f9f8f6',
          bodyColor: dark ? '#1a1a1a' : '#f9f8f6',
          padding: 12,
          cornerRadius: 4,
          displayColors: false,
          callbacks: {
            label: (context) =>
              format(
                Number(
                  this.type === 'doughnut'
                    ? context.raw
                    : this.horizontal
                      ? context.parsed.x
                      : context.parsed.y,
                ),
              ),
          },
        },
      },
    };
    if (this.type !== 'doughnut') {
      const valueAxis = {
        beginAtZero: true,
        border: { display: false },
        grid: { color: grid },
        ticks: {
          color: muted,
          font: { family: 'Inter', size: 11 },
          precision: this.currency ? undefined : 0,
          callback: (value: string | number) => compact(Number(value)),
        },
      };
      const labelAxis = {
        border: { display: false },
        grid: { display: false },
        ticks: {
          color: muted,
          font: { family: 'Inter', size: 11 },
          maxRotation: 0,
          autoSkip: true,
          maxTicksLimit: 7,
        },
      };
      options.scales = this.horizontal
        ? { x: valueAxis, y: labelAxis }
        : { x: labelAxis, y: valueAxis };
    }
    if (this.type === 'doughnut') Object.assign(options, { cutout: '76%' });
    this.chart = new this.chartConstructor(this.canvas.nativeElement, {
      type: this.type,
      data: {
        labels: this.labels,
        datasets: [
          {
            label: this.label,
            data: this.values,
            borderColor: this.type === 'line' ? '#b48b42' : 'transparent',
            backgroundColor:
              this.type === 'line'
                ? 'rgba(180,139,66,.12)'
                : this.colors.length
                  ? this.colors
                  : [
                      '#b48b42',
                      '#7e796c',
                      '#9eae9a',
                      '#c9b58e',
                      '#62665f',
                      '#d7cdbd',
                    ],
            borderWidth: this.type === 'line' ? 2 : 0,
            fill: this.type === 'line',
            tension: 0.28,
            pointRadius: this.values.length > 60 ? 0 : 2,
            pointHoverRadius: 5,
            pointBackgroundColor: '#b48b42',
            ...(this.type === 'bar'
              ? { borderRadius: 3, maxBarThickness: 24 }
              : {}),
          },
        ],
      },
      options,
    });
  }
}
