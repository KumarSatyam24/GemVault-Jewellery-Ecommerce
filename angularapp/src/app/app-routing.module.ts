import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CatalogComponent } from './components/catalog/catalog.component';
import { BuyersComponent } from './components/buyers/buyers.component';
import { OrdersComponent } from './components/orders/orders.component';
import { InventoryComponent } from './components/inventory/inventory.component';
import { AdminOrdersComponent } from './components/admin-orders/admin-orders.component';
import { RestockComponent } from './components/restock/restock.component';
import { FeedbackComponent } from './components/feedback/feedback.component';
import { ErrorComponent } from './components/error/error.component';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { ProfileComponent } from './components/profile/profile.component';
import { ProductDetailComponent } from './components/product-detail/product-detail.component';
import { EmailActionComponent } from './components/email-action/email-action.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { OauthCallbackComponent } from './components/oauth-callback/oauth-callback.component';
import { WishlistComponent } from './components/wishlist/wishlist.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AuthGuard } from './services/auth.guard';
import { GuestGuard } from './services/guest.guard';
const routes: Routes = [
  { path: 'admin/dashboard', component: DashboardComponent, canActivate: [AuthGuard], data: { role: 'Admin' } },
  { path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
  { path: 'signup', component: SignupComponent, canActivate: [GuestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: 'verify-email',
    component: EmailActionComponent,
    data: { action: 'verify-email' },
  },
  {
    path: 'admin-login',
    component: EmailActionComponent,
    data: { action: 'admin-login' },
  },
  { path: 'oauth/callback', component: OauthCallbackComponent },
  {
    path: 'wishlist',
    component: WishlistComponent,
    canActivate: [AuthGuard],
    data: { role: 'User' },
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [AuthGuard],
    data: { role: 'User' },
  },
  { path: '', component: CatalogComponent },
  { path: 'products/:id', component: ProductDetailComponent },
  {
    path: 'buyers',
    component: BuyersComponent,
    canActivate: [AuthGuard],
    data: { role: 'Admin' },
  },
  {
    path: 'orders',
    component: OrdersComponent,
    canActivate: [AuthGuard],
    data: { role: 'User' },
  },
  { path: 'feedback', component: FeedbackComponent, canActivate: [AuthGuard] },
  {
    path: 'admin/items',
    component: InventoryComponent,
    canActivate: [AuthGuard],
    data: { role: 'Admin' },
  },
  {
    path: 'admin/orders',
    component: AdminOrdersComponent,
    canActivate: [AuthGuard],
    data: { role: 'Admin' },
  },
  {
    path: 'admin/restock',
    component: RestockComponent,
    canActivate: [AuthGuard],
    data: { role: 'Admin' },
  },
  { path: 'error', component: ErrorComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'error' },
];
@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'enabled',
      anchorScrolling: 'enabled',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
