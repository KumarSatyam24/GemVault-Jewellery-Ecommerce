import { Component } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';
@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent {
  error = '';
  message = '';
  registered = false;
  saving = false;
  showPassword = false;
  showConfirmPassword = false;
  form = this.fb.group(
    {
      username: [
        '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(/.*\S.*/),
        ],
      ],
      email: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[6-9][0-9]{9}$/)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/,
          ),
        ],
      ],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: (form: AbstractControl): ValidationErrors | null =>
        form.get('password')?.value === form.get('confirmPassword')?.value
          ? null
          : { passwordMismatch: true },
    },
  );
  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
  ) {}
  register(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    const { confirmPassword, ...user } = this.form.getRawValue();
    this.auth.register({ ...user, userRole: 'User' } as User).subscribe({
      next: () => {
        this.saving = false;
        this.registered = true;
        this.message = 'Account created. Check your email to verify it before signing in.';
      },
      error: (e) => {
        this.saving = false;
        this.error = e.error?.message || 'Could not create account.';
      },
    });
  }
  resend(): void {
    const email = this.form.controls.email.value;
    if (!email || this.form.controls.email.invalid) return;
    this.saving = true;
    this.auth.resendVerification(email).subscribe({
      next: (result) => {
        this.message = result.message;
        this.saving = false;
      },
      error: (e) => {
        this.error = e.error?.message || 'Could not resend verification.';
        this.saving = false;
      },
    });
  }
  signInWithGoogle(): void {
    window.location.assign(this.auth.googleSignInUrl());
  }
}
