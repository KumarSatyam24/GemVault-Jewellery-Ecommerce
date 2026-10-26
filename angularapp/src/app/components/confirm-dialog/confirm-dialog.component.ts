import { Component, HostListener } from '@angular/core';
import { ConfirmationService } from '../../services/confirmation.service';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  constructor(public confirmation: ConfirmationService) {}

  @HostListener('document:keydown.escape')
  escape(): void {
    this.confirmation.answer(false);
  }
}
