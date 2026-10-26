import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  error = '';
  message = '';
  saving = false;
  showPassword = false;
  awaitingAdminCode = false;
  adminCode = '';
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}
  ngOnInit(): void {
    const oauthError = this.route.snapshot.queryParamMap.get('oauthError');
    if (oauthError === 'account_not_eligible') {
      this.error =
        'Google sign-in is available for buyer accounts only. Administrators must use password sign-in.';
    } else if (oauthError === 'missing_verified_email') {
      this.error =
        'Google did not provide a verified email address for this account.';
    } else if (oauthError === 'google_failed') {
      this.error =
        'Google could not complete sign-in. Please try again or use password sign-in.';
    }
  }
  login(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: (result) => {
        this.saving = false;
        if (!('token' in result)) {
          this.message = result.message;
          this.awaitingAdminCode = true;
          return;
        }
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const safeReturn =
          returnUrl?.startsWith('/') && !returnUrl.startsWith('//')
            ? returnUrl
            : null;
        this.router.navigateByUrl(
          result.userRole === 'Admin' ? '/admin/dashboard' : safeReturn || '/',
        );
      },
      error: (e) => {
        this.saving = false;
        this.error = e.error?.message || 'Invalid email or password.';
      },
    });
  }
  verifyCode(): void {
    if (!/^\d{6}$/.test(this.adminCode)) {
      this.error = 'Enter the 6-digit code from your email.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.auth
      .verifyAdminLogin(this.form.controls.email.value || '', this.adminCode)
      .subscribe({
        next: () => {
          this.saving = false;
          this.router.navigateByUrl('/admin/dashboard');
        },
        error: (e) => {
          this.saving = false;
          this.error = e.error?.message || 'The code is incorrect or expired.';
        },
      });
  }
  signInWithGoogle(): void {
    window.location.assign(this.auth.googleSignInUrl());
  }
}
