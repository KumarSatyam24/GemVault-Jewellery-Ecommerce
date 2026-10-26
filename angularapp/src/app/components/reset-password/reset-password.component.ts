import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  error = '';
  message = '';
  saving = false;
  showPassword = false;
  showConfirm = false;
  form = this.fb.group(
    {
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
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
  ) {}
  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token)
      this.error = 'This reset link is incomplete. Request a new one.';
  }
  submit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.auth
      .resetPassword(this.token, this.form.controls.password.value || '')
      .subscribe({
        next: (result) => {
          this.message = result.message;
          this.saving = false;
          setTimeout(() => this.router.navigate(['/login']), 1300);
        },
        error: (event) => {
          this.error = event.error?.message || 'Could not reset the password.';
          this.saving = false;
        },
      });
  }
}
