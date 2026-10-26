import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-oauth-callback',
  templateUrl: './oauth-callback.component.html',
})
export class OauthCallbackComponent implements OnInit {
  error = '';

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const fragment = window.location.hash.startsWith('#')
      ? window.location.hash.slice(1)
      : '';
    const token = new URLSearchParams(fragment).get('token');
    if (!token || !this.auth.acceptGoogleToken(token)) {
      this.error = 'Google sign-in could not be completed. Please try again.';
      return;
    }
    this.router.navigate([this.auth.isAdmin() ? '/admin/dashboard' : '/']);
  }
}
