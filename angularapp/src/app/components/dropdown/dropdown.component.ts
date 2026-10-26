import {
  Component,
  ElementRef,
  HostListener,
  Input,
  ViewChild,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface DropdownOption {
  label: string;
  value: string | number | null;
}

@Component({
  selector: 'app-dropdown',
  templateUrl: './dropdown.component.html',
  styleUrls: ['./dropdown.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DropdownComponent),
      multi: true,
    },
  ],
})
export class DropdownComponent implements ControlValueAccessor {
  @ViewChild('trigger') trigger?: ElementRef<HTMLButtonElement>;
  @Input() options: DropdownOption[] = [];
  @Input() placeholder = 'Select an option';
  @Input() label = 'Select an option';
  value: string | number | null = null;
  open = false;
  disabled = false;
  activeIndex = 0;
  private onChange: (value: string | number | null) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private element: ElementRef<HTMLElement>) {}

  get selectedLabel(): string {
    return (
      this.options.find((option) => option.value === this.value)?.label ||
      this.placeholder
    );
  }

  writeValue(value: string | number | null): void {
    this.value = value;
  }
  registerOnChange(fn: (value: string | number | null) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }
  toggle(): void {
    if (this.disabled) return;
    this.open = !this.open;
    this.activeIndex = Math.max(
      0,
      this.options.findIndex((option) => option.value === this.value),
    );
    this.onTouched();
  }
  choose(option: DropdownOption): void {
    this.value = option.value;
    this.onChange(option.value);
    this.onTouched();
    this.open = false;
    this.trigger?.nativeElement.focus();
  }
  @HostListener('document:click', ['$event'])
  outside(event: MouseEvent): void {
    if (!this.element.nativeElement.contains(event.target as Node))
      this.open = false;
  }
  @HostListener('keydown', ['$event'])
  keydown(event: KeyboardEvent): void {
    if (this.disabled) return;
    if (event.key === 'Escape') {
      this.open = false;
      return;
    }
    if (
      this.options.length &&
      (event.key === 'ArrowDown' || event.key === 'ArrowUp')
    ) {
      event.preventDefault();
      this.open = true;
      this.activeIndex =
        (this.activeIndex +
          (event.key === 'ArrowDown' ? 1 : -1) +
          this.options.length) %
        this.options.length;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (this.open && this.options[this.activeIndex])
        this.choose(this.options[this.activeIndex]);
      else this.toggle();
    }
  }
}
