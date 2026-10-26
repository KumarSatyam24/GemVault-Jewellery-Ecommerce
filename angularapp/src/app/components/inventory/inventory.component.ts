import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { JewelleryItem } from '../../models/jewellery-item.model';
import { JewelleryService } from '../../services/jewellery.service';
import { DropdownOption } from '../dropdown/dropdown.component';
import { ConfirmationService } from '../../services/confirmation.service';
import { handleProductImageError } from '../../config/product-image.config';
@Component({
  selector: 'app-inventory',
  templateUrl: './inventory.component.html',
})
export class InventoryComponent implements OnInit {
  imageFailed = handleProductImageError;
  items: JewelleryItem[] = [];
  loading = true;
  formOpen = false;
  get activeCount(): number {
    return this.items.filter((item) => item.active !== false).length;
  }
  get lowStockCount(): number {
    return this.items.filter(
      (item) =>
        item.stockQuantity <= item.reorderThreshold && item.active !== false,
    ).length;
  }
  categories = ['Ring', 'Necklace', 'Bangle', 'Earring', 'Pendant', 'Chain'];
  materials = ['Gold', 'Silver', 'Platinum', 'Diamond'];
  goldPurities = [
    '24K / 999',
    '23K / 958',
    '22K / 916',
    '21K / 875',
    '20K / 833',
    '18K / 750',
    '14K / 585',
    '10K / 417',
    '9K / 375',
  ];
  silverPurities = [
    '999 Fine Silver',
    '958 Britannia Silver',
    '950 Silver',
    '925 Sterling Silver',
    '900 Coin Silver',
    '835 Silver',
    '800 Silver',
  ];
  diamondClarities = ['FL', 'IF', 'VVS1', 'VVS2', 'VS1', 'VS2', 'SI1', 'SI2'];
  get categoryOptions(): DropdownOption[] {
    return [
      { label: 'Select category', value: '' },
      ...this.categories.map((value) => ({ label: value, value })),
    ];
  }
  get materialOptions(): DropdownOption[] {
    return [
      { label: 'Select material', value: '' },
      ...this.materials.map((value) => ({ label: value, value })),
    ];
  }
  get purityOptions(): DropdownOption[] {
    const values =
      this.form.controls.material.value === 'Gold'
        ? this.goldPurities
        : this.form.controls.material.value === 'Silver'
          ? this.silverPurities
          : ['999 Platinum', '950 Platinum', 'Diamond grade / mixed metal'];
    return [
      { label: 'Select purity', value: '' },
      ...values.map((value) => ({ label: value, value })),
    ];
  }
  get clarityOptions(): DropdownOption[] {
    return [
      { label: 'No diamond clarity', value: '' },
      ...this.diamondClarities.map((value) => ({ label: value, value })),
    ];
  }
  editingId?: number;
  saving = false;
  selectedFileName = '';
  importing = false;
  error = '';
  message = '';
  form = this.fb.group({
    itemName: ['', Validators.required],
    sku: ['', Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9-]{2,39}$/)],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    imageUrl: [
      '',
      [
        Validators.required,
        Validators.pattern(
          /^(https?:\/\/\S+|data:image\/(png|jpeg|webp);base64,)/i,
        ),
      ],
    ],
    category: ['', Validators.required],
    material: ['', Validators.required],
    purity: [''],
    stoneClarity: [''],
    weightInGrams: [0, [Validators.required, Validators.min(0.01)]],
    stoneWeightInGrams: [0, [Validators.required, Validators.min(0)]],
    makingCharges: [0, [Validators.required, Validators.min(0)]],
    pricePerGram: [0, [Validators.required, Validators.min(0)]],
    stoneValue: [0, [Validators.required, Validators.min(0)]],
    vaCharges: [0, [Validators.required, Validators.min(0)]],
    discountAmount: [0, [Validators.required, Validators.min(0)]],
    cgstRate: [1.5, [Validators.required, Validators.min(0)]],
    sgstRate: [1.5, [Validators.required, Validators.min(0)]],
    roundOff: [0, Validators.required],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
    reorderThreshold: [0, [Validators.required, Validators.min(0)]],
  });
  constructor(
    private fb: FormBuilder,
    private jewellery: JewelleryService,
    private confirmation: ConfirmationService,
  ) {}
  ngOnInit(): void {
    this.load();
    this.form.controls.material.valueChanges.subscribe(() => {
      this.form.controls.purity.setValue('');
    });
    this.form.controls.purity.valueChanges.subscribe(() => {
      const material = this.form.controls.material.value || '';
      if (
        ['Gold', 'Silver'].includes(material) &&
        this.form.controls.purity.value
      )
        this.useLiveRate();
    });
  }
  invalid(key: 'itemName' | 'imageUrl'): boolean {
    const c = this.form.controls[key];
    return c.touched && c.invalid;
  }
  load(): void {
    this.loading = true;
    this.jewellery.getAllItems().subscribe({
      next: (x) => {
        this.items = x;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load items.';
        this.loading = false;
      },
    });
  }
  edit(item: JewelleryItem): void {
    this.formOpen = true;
    this.editingId = item.itemId;
    this.form.patchValue(item);
    this.message = '';
    window.scrollTo(0, 0);
  }
  reset(): void {
    this.formOpen = false;
    this.editingId = undefined;
    this.selectedFileName = '';
    this.form.reset({
      itemName: '',
      sku: '',
      description: '',
      imageUrl: '',
      category: '',
      material: '',
      purity: '',
      stoneClarity: '',
      weightInGrams: 0,
      stoneWeightInGrams: 0,
      makingCharges: 0,
      pricePerGram: 0,
      stoneValue: 0,
      vaCharges: 0,
      discountAmount: 0,
      cgstRate: 1.5,
      sgstRate: 1.5,
      roundOff: 0,
      stockQuantity: 0,
      reorderThreshold: 0,
    });
  }
  useLiveRate(): void {
    const material = this.form.controls.material.value || '';
    const purity = this.form.controls.purity.value || '';
    if (!['Gold', 'Silver'].includes(material)) {
      this.error =
        'Live rates are available for Gold and Silver. Enter the rate for other materials.';
      return;
    }
    this.jewellery.getLiveMetalRate(material, purity).subscribe({
      next: (quote) => {
        this.form.controls.pricePerGram.setValue(quote.ratePerGram);
        this.message = `${quote.material} rate updated: ₹${quote.ratePerGram.toLocaleString('en-IN')} per gram.`;
        this.error = '';
      },
      error: (response) =>
        (this.error =
          response.error?.message ||
          'Could not fetch a live rate. Enter the rate manually.'),
    });
  }
  get billingPreview(): {
    netWeight: number;
    metalValue: number;
    taxable: number;
    cgst: number;
    sgst: number;
    total: number;
  } {
    const value = this.form.getRawValue();
    const number = (input: number | null | undefined) => Number(input) || 0;
    const netWeight = Math.max(
      0,
      number(value.weightInGrams) - number(value.stoneWeightInGrams),
    );
    const metalValue = netWeight * number(value.pricePerGram);
    const taxable = Math.max(
      0,
      metalValue +
        number(value.stoneValue) +
        number(value.makingCharges) +
        number(value.vaCharges) -
        number(value.discountAmount),
    );
    const cgst = (taxable * number(value.cgstRate)) / 100;
    const sgst = (taxable * number(value.sgstRate)) / 100;
    return {
      netWeight,
      metalValue,
      taxable,
      cgst,
      sgst,
      total: taxable + cgst + sgst + number(value.roundOff),
    };
  }
  imageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.error = 'Choose a JPG, PNG, or WebP image.';
      this.selectedFileName = '';
      input.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.error = 'Choose an image smaller than 2 MB.';
      this.selectedFileName = '';
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      this.form.controls.imageUrl.setValue(String(reader.result));
      this.form.controls.imageUrl.markAsDirty();
      this.form.controls.imageUrl.markAsTouched();
      this.selectedFileName = file.name;
      this.error = '';
      input.value = '';
    };
    reader.onerror = () => {
      this.error = 'The image could not be read. Please try another file.';
      this.selectedFileName = '';
      input.value = '';
    };
    reader.readAsDataURL(file);
  }
  downloadTemplate(): void {
    this.jewellery.csvTemplate().subscribe((file) => {
      const url = URL.createObjectURL(file);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'gemvault-products-template.csv';
      link.click();
      URL.revokeObjectURL(url);
    });
  }
  downloadExport(): void {
    this.jewellery.exportCsv().subscribe({
      next: (file) => {
        const url = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'gemvault-products.csv';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => (this.error = 'Could not export the product list.'),
    });
  }
  csvSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.error = 'Choose a CSV file.';
      return;
    }
    this.importing = true;
    this.jewellery.importCsv(file).subscribe({
      next: (result) => {
        this.importing = false;
        this.message = `CSV complete: ${result.created} created, ${result.updated} updated.`;
        this.error = result.errors.length ? result.errors.join(' ') : '';
        this.load();
      },
      error: (response) => {
        this.importing = false;
        this.error = response.error?.message || 'Could not import CSV.';
      },
    });
  }
  clearImage(): void {
    this.form.controls.imageUrl.setValue('');
    this.form.controls.imageUrl.markAsTouched();
    this.selectedFileName = '';
  }
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    const item = this.form.getRawValue() as JewelleryItem;
    const request = this.editingId
      ? this.jewellery.updateItem(this.editingId, item)
      : this.jewellery.addItem(item);
    request.subscribe({
      next: () => {
        this.message = 'Item saved.';
        this.saving = false;
        this.reset();
        this.load();
      },
      error: (e) => {
        this.error = e.error?.message || 'Could not save item.';
        this.saving = false;
      },
    });
  }
  async remove(item: JewelleryItem): Promise<void> {
    if (
      !item.itemId ||
      !(await this.confirmation.confirm(
        'Delete this piece?',
        `${item.itemName} will be removed if it has no order history. Booked pieces should be archived.`,
        'Delete piece',
      ))
    )
      return;
    this.error = '';
    this.jewellery.deleteItem(item.itemId).subscribe({
      next: () => {
        this.message = 'Item deleted.';
        this.load();
      },
      error: (e) => (this.error = e.error?.message || 'Could not delete item.'),
    });
  }
  setArchived(item: JewelleryItem, archive: boolean): void {
    if (!item.itemId) return;
    const request = archive
      ? this.jewellery.archiveItem(item.itemId)
      : this.jewellery.restoreItem(item.itemId);
    request.subscribe({
      next: () => {
        this.message = archive
          ? 'Item archived. Existing orders remain available.'
          : 'Item restored.';
        this.error = '';
        this.load();
      },
      error: (e) =>
        (this.error =
          e.error?.message || 'Could not change item availability.'),
    });
  }
}
