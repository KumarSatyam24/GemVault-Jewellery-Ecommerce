import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Feedback } from '../../models/feedback.model';
import { JewelleryItem } from '../../models/jewellery-item.model';
import { FeedbackService } from '../../services/feedback.service';
import { JewelleryService } from '../../services/jewellery.service';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute } from '@angular/router';
import { DropdownOption } from '../dropdown/dropdown.component';
import { forkJoin } from 'rxjs';
import { ConfirmationService } from '../../services/confirmation.service';
@Component({
  selector: 'app-feedback',
  templateUrl: './feedback.component.html',
  styleUrls: ['./feedback.component.css'],
})
export class FeedbackComponent implements OnInit {
  items: JewelleryItem[] = [];
  feedbacks: Feedback[] = [];
  loading = true;
  error = '';
  message = '';
  saving = false;
  readonly stars = [1, 2, 3, 4, 5];
  get itemOptions(): DropdownOption[] {
    return [
      { label: 'Select a purchased piece', value: null },
      ...this.items.map((item) => ({
        label: item.itemName,
        value: item.itemId || 0,
      })),
    ];
  }
  form = this.fb.group({
    itemId: [null as number | null, Validators.required],
    feedbackText: ['', Validators.required],
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
  });
  constructor(
    private fb: FormBuilder,
    private feedback: FeedbackService,
    private jewellery: JewelleryService,
    public auth: AuthService,
    private route: ActivatedRoute,
    private confirmation: ConfirmationService,
  ) {}
  ngOnInit(): void {
    this.load();
    this.loadEligible();
  }
  loadEligible(): void {
    if (!this.auth.isCustomer()) return;
    forkJoin({
      orders: this.jewellery.getOrdersByUserId(this.auth.getUserId()),
      items: this.jewellery.getAllItems(),
      ownReviews: this.feedback.getAllfeedbacksByUserId(this.auth.getUserId()),
    }).subscribe({
      next: ({ orders, items, ownReviews }) => {
        const delivered = new Set(
          orders
            .filter((order) => order.status === 'Delivered')
            .map((order) => order.itemId),
        );
        const reviewed = new Set(ownReviews.map((review) => review.itemId));
        this.items = items.filter(
          (item) =>
            !!item.itemId &&
            delivered.has(item.itemId) &&
            !reviewed.has(item.itemId),
        );
        const requested = Number(
          this.route.snapshot.queryParamMap.get('itemId'),
        );
        if (this.items.some((item) => item.itemId === requested))
          this.form.controls.itemId.setValue(requested);
      },
      error: () => (this.error = 'Could not load delivered pieces.'),
    });
  }
  load(): void {
    this.loading = true;
    this.feedback.getFeedbacks().subscribe({
      next: (x) => {
        this.feedbacks = x;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load feedback.';
        this.loading = false;
      },
    });
  }
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (
      !this.items.some(
        (item) => item.itemId === this.form.controls.itemId.value,
      )
    ) {
      this.error = 'Select a piece from your purchase history.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.feedback
      .sendFeedback({
        ...this.form.getRawValue(),
      } as Feedback)
      .subscribe({
        next: () => {
          this.message = 'Feedback added.';
          this.form.reset();
          this.saving = false;
          this.load();
          this.loadEligible();
        },
        error: (e) => {
          this.error = e.error?.message || 'Could not save feedback.';
          this.saving = false;
        },
      });
  }
  async remove(entry: Feedback): Promise<void> {
    if (
      !entry.feedbackId ||
      !(await this.confirmation.confirm(
        'Delete your review?',
        `Your review of ${entry.item?.itemName || 'this piece'} will be removed.`,
        'Delete review',
      ))
    )
      return;
    this.feedback.deleteFeedback(entry.feedbackId).subscribe({
      next: () => {
        this.message = 'Feedback deleted.';
        this.load();
        this.loadEligible();
      },
      error: (e) =>
        (this.error = e.error?.message || 'Could not delete feedback.'),
    });
  }
}
