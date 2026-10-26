import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent {
  message = '';
  error = '';
  saving = false;
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });
  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
  ) {}
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.auth
      .requestPasswordReset(this.form.controls.email.value || '')
      .subscribe({
        next: (result) => {
          this.message = result.message;
          this.saving = false;
        },
        error: (event) => {
          this.error =
            event.error?.message || 'Unable to request a reset link.';
          this.saving = false;
        },
      });
  }
}
