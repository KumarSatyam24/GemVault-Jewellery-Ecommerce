import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { SavedAddress } from '../../models/saved-address.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ConfirmationService } from '../../services/confirmation.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  email = '';
  addresses: SavedAddress[] = [];
  editingAddressId?: number;
  error = '';
  message = '';
  saving = false;
  addressLabelChoice = 'Home';
  customAddressLabel = '';
  profileForm = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    mobileNumber: ['', Validators.pattern(/^$|^[6-9][0-9]{9}$/)],
  });
  addressForm = this.fb.group({
    label: ['', [Validators.required, Validators.maxLength(40)]],
    line1: ['', [Validators.required, Validators.maxLength(250)]],
    city: ['', [Validators.required, Validators.maxLength(80)]],
    state: ['', [Validators.required, Validators.maxLength(80)]],
    postalCode: ['', [Validators.required, Validators.maxLength(12)]],
  });

  constructor(
    private fb: FormBuilder,
    private users: UserService,
    private auth: AuthService,
    private confirmation: ConfirmationService,
  ) {}
  ngOnInit(): void {
    this.users.getProfile().subscribe({
      next: (user) => {
        this.email = user.email;
        this.profileForm.patchValue({
          username: user.username,
          mobileNumber: user.mobileNumber,
        });
      },
      error: () => (this.error = 'Could not load profile.'),
    });
    this.loadAddresses();
  }
  loadAddresses(): void {
    this.users.getAddresses().subscribe({
      next: (addresses) => (this.addresses = addresses),
      error: () => (this.error = 'Could not load addresses.'),
    });
  }
  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.users
      .updateProfile(
        this.profileForm.getRawValue() as {
          username: string;
          mobileNumber: string;
        },
      )
      .subscribe({
        next: (user) => {
          this.saving = false;
          this.message = 'Profile saved.';
          this.error = '';
          this.auth.updateUsername(user.username);
        },
        error: (error) => {
          this.saving = false;
          this.error = error.error?.message || 'Could not save profile.';
        },
      });
  }
  editAddress(address: SavedAddress): void {
    this.editingAddressId = address.addressId;
    this.addressForm.patchValue(address);
    this.addressLabelChoice = ['Home', 'Office'].includes(address.label || '')
      ? address.label!
      : 'Other';
    this.customAddressLabel =
      this.addressLabelChoice === 'Other' ? address.label || '' : '';
    this.message = '';
  }
  resetAddress(): void {
    this.editingAddressId = undefined;
    this.addressLabelChoice = 'Home';
    this.customAddressLabel = '';
    this.addressForm.reset({
      label: '',
      line1: '',
      city: '',
      state: '',
      postalCode: '',
    });
  }
  saveAddress(): void {
    this.addressForm.controls.label.setValue(
      this.addressLabelChoice === 'Other'
        ? this.customAddressLabel.trim()
        : this.addressLabelChoice,
    );
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const address = this.addressForm.getRawValue() as SavedAddress;
    const request = this.editingAddressId
      ? this.users.updateAddress(this.editingAddressId, address)
      : this.users.addAddress(address);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Address saved.';
        this.error = '';
        this.resetAddress();
        this.loadAddresses();
      },
      error: (error) => {
        this.saving = false;
        this.error = error.error?.message || 'Could not save address.';
      },
    });
  }
  async deleteAddress(address: SavedAddress): Promise<void> {
    if (
      !address.addressId ||
      !(await this.confirmation.confirm(
        'Delete saved address?',
        `${address.label} will be removed from your saved addresses. Existing order history keeps its delivery address.`,
        'Delete address',
      ))
    )
      return;
    this.users.deleteAddress(address.addressId).subscribe({
      next: () => {
        this.message = 'Address deleted.';
        this.loadAddresses();
      },
      error: (error) =>
        (this.error = error.error?.message || 'Could not delete address.'),
    });
  }
}
