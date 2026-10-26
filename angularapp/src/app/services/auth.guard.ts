import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  Router,
  UrlTree,
} from '@angular/router';
import { AuthService } from './auth.service';
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}
  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (!this.auth.isAuthenticated()) return this.router.parseUrl('/login');
    const role = route.data['role'] as string | undefined;
    if (role === 'Admin' && !this.auth.isAdmin())
      return this.router.parseUrl('/error');
    if (role === 'User' && !this.auth.isCustomer())
      return this.router.parseUrl('/error');
    return true;
  }
}
