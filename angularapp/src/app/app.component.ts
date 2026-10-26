import { Component, HostListener, OnInit } from '@angular/core';
import { AuthService } from './services/auth.service';
import { CartService } from './services/cart.service';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { DropdownOption } from './components/dropdown/dropdown.component';
import { JewelleryService } from './services/jewellery.service';
import { UserService } from './services/user.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  menuOpen = false;
  cartOpen = false;
  darkMode = localStorage.getItem('gemvault.theme') === 'dark';
  selectedMetal = 'Gold';
  selectedPurity = '22K / 916';
  liveRate?: number;
  rateError = '';
  ratePanelOpen = false;
  readonly goldPurities = [
    '24K / 999',
    '23K / 958',
    '22K / 916',
    '21K / 875',
    '20K / 833',
    '18K / 750',
    '14K / 585',
    '10K / 417',
    '9K / 375',
  ];
  readonly silverPurities = [
    '999 Fine Silver',
    '958 Britannia Silver',
    '950 Silver',
    '925 Sterling Silver',
    '900 Coin Silver',
    '835 Silver',
    '800 Silver',
  ];
  constructor(
    public auth: AuthService,
    public cart: CartService,
    private router: Router,
    private jewellery: JewelleryService,
    private users: UserService,
  ) {
    this.applyTheme();
  }
  ngOnInit(): void {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        if (this.auth.isAuthenticated() && !this.isAuthPage)
          this.users.getProfile().subscribe({ error: () => {} });
        if (
          this.auth.isAuthenticated() &&
          !this.isAuthPage &&
          this.liveRate === undefined
        )
          this.refreshLiveRate();
      });
    if (this.auth.isAuthenticated() && !this.isAuthPage) {
      this.users.getProfile().subscribe({ error: () => {} });
      this.refreshLiveRate();
    }
  }
  get metalOptions(): DropdownOption[] {
    return [
      { label: 'Gold', value: 'Gold' },
      { label: 'Silver', value: 'Silver' },
    ];
  }
  get purityOptions(): DropdownOption[] {
    const values =
      this.selectedMetal === 'Gold' ? this.goldPurities : this.silverPurities;
    return values.map((value) => ({ label: value, value }));
  }
  onMetalChange(): void {
    this.selectedPurity =
      this.selectedMetal === 'Gold' ? '22K / 916' : '925 Sterling Silver';
    this.refreshLiveRate();
  }
  refreshLiveRate(forceRefresh = false): void {
    if (!this.auth.isAuthenticated()) return;
    this.rateError = '';
    this.jewellery
      .getLiveMetalRate(this.selectedMetal, this.selectedPurity, forceRefresh)
      .subscribe({
        next: (quote) => (this.liveRate = quote.ratePerGram),
        error: (response) => {
          this.liveRate = undefined;
          this.rateError = response.error?.message || 'Rate unavailable';
        },
      });
  }
  @HostListener('document:click', ['$event'])
  closeRatePanelOnOutsideClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (this.ratePanelOpen && !target?.closest('.live-rate'))
      this.ratePanelOpen = false;
  }
  toggleTheme(): void {
    this.darkMode = !this.darkMode;
    this.applyTheme();
  }
  private applyTheme(): void {
    document.documentElement.dataset['theme'] = this.darkMode
      ? 'dark'
      : 'light';
    document.documentElement.classList.toggle('dark', this.darkMode);
    localStorage.setItem('gemvault.theme', this.darkMode ? 'dark' : 'light');
  }
  logout(): void {
    this.menuOpen = false;
    this.cartOpen = false;
    this.auth.logout();
  }
  get isAuthPage(): boolean {
    return (
      this.router.url.startsWith('/login') ||
      this.router.url.startsWith('/signup')
    );
  }
}
