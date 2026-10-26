import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}
  intercept(
    request: HttpRequest<unknown>,
    next: HttpHandler,
  ): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('token');
    const authenticatedRequest = !!(
      token &&
      request.url.includes('/api/') &&
      !request.url.endsWith('/api/login') &&
      !request.url.endsWith('/api/register')
    );
    if (authenticatedRequest)
      request = request.clone({
        setHeaders: { Authorization: 'Bearer ' + token },
      });
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (
          authenticatedRequest &&
          error.status === 401 &&
          localStorage.getItem('token') === token
        ) {
          ['token', 'username', 'userRole', 'userId'].forEach((key) =>
            localStorage.removeItem(key),
          );
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      }),
    );
  }
}
