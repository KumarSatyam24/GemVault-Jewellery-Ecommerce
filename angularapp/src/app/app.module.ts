import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
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
import { CartSidebarComponent } from './components/cart-sidebar/cart-sidebar.component';
import { ProductDetailComponent } from './components/product-detail/product-detail.component';
import { DropdownComponent } from './components/dropdown/dropdown.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { EmailActionComponent } from './components/email-action/email-action.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { OauthCallbackComponent } from './components/oauth-callback/oauth-callback.component';
import { InvoiceComponent } from './components/invoice/invoice.component';
import { WishlistComponent } from './components/wishlist/wishlist.component';
import { ChatbotComponent } from './components/chatbot/chatbot.component';
import { AuthInterceptor } from './services/auth.interceptor';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { DashboardChartComponent } from './components/dashboard-chart/dashboard-chart.component';
@NgModule({
  declarations: [
    AppComponent,
    CatalogComponent,
    BuyersComponent,
    OrdersComponent,
    InventoryComponent,
    AdminOrdersComponent,
    RestockComponent,
    FeedbackComponent,
    ErrorComponent,
    LoginComponent,
    SignupComponent,
    ProfileComponent,
    CartSidebarComponent,
    ProductDetailComponent,
    DropdownComponent,
    ConfirmDialogComponent,
    EmailActionComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    OauthCallbackComponent,
    InvoiceComponent,
    WishlistComponent,
    ChatbotComponent,
    DashboardComponent,
    DashboardChartComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    AppRoutingModule,
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  bootstrap: [AppComponent],
})
export class AppModule {}
