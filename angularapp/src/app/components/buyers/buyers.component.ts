import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
@Component({
  selector: 'app-buyers',
  templateUrl: './buyers.component.html',
  styleUrls: ['./buyers.component.css'],
})
export class BuyersComponent {
  error = '';
  message = '';
  saving = false;
  form = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    mobileNumber: ['', Validators.pattern(/^$|^[6-9][0-9]{9}$/)],
  });
  constructor(
    private fb: FormBuilder,
    private users: UserService,
  ) {}
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    this.users
      .inviteBuyer(
        this.form.getRawValue() as {
          username: string;
          email: string;
          mobileNumber: string;
        },
      )
      .subscribe({
        next: (user) => {
          this.message =
            'Invitation sent to ' +
            user.email +
            '. They will choose a password from the email link.';
          this.form.reset();
          this.saving = false;
        },
        error: (e) => {
          this.error = e.error?.message || 'Could not add buyer.';
          this.saving = false;
        },
      });
  }
}
