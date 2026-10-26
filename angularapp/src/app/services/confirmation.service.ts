import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ConfirmationRequest {
  title: string;
  message: string;
  action: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private requestSubject = new BehaviorSubject<ConfirmationRequest | null>(
    null,
  );
  request$ = this.requestSubject.asObservable();
  private resolve?: (accepted: boolean) => void;

  confirm(
    title: string,
    message: string,
    action = 'Confirm',
  ): Promise<boolean> {
    this.answer(false);
    return new Promise((resolve) => {
      this.resolve = resolve;
      this.requestSubject.next({ title, message, action });
    });
  }

  answer(accepted: boolean): void {
    this.resolve?.(accepted);
    this.resolve = undefined;
    this.requestSubject.next(null);
  }
}
