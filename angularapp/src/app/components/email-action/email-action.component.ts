import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-email-action',
  templateUrl: './email-action.component.html',
})
export class EmailActionComponent implements OnInit {
  title = 'Verifying your email';
  message = 'Please wait while we confirm your secure link.';
  error = '';
  loading = true;
  adminLogin = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token') || '';
    this.adminLogin = this.route.snapshot.data['action'] === 'admin-login';
    if (this.adminLogin) {
      this.title = 'Admin sign-in has changed';
      this.fail(
        'Sign in again and enter the one-time code sent to your email. Old sign-in links no longer work.',
      );
      return;
    }
    this.title = this.adminLogin
      ? 'Confirming admin sign-in'
      : 'Verifying your email';
    if (!token) {
      this.fail('This link is incomplete. Request a new email and try again.');
      return;
    }
    const onSuccess = (response: any) => {
      this.loading = false;
      this.message = this.adminLogin
        ? 'Signed in securely. Opening the admin area…'
        : response.message;
      setTimeout(
        () =>
          this.router.navigate([
            this.adminLogin ? '/admin/dashboard' : '/login',
          ]),
        1200,
      );
    };
    const onError = (event: any) =>
      this.fail(
        event.error?.message ||
          'This link is invalid, expired, or already used.',
      );
    this.auth.verifyEmail(token).subscribe({ next: onSuccess, error: onError });
  }

  private fail(message: string): void {
    this.loading = false;
    this.error = message;
  }
}
