import { Component, Input } from '@angular/core';
import { JewelleryOrder } from '../../models/jewellery-order.model';

@Component({
  selector: 'app-invoice',
  templateUrl: './invoice.component.html',
})
export class InvoiceComponent {
  @Input() orders: JewelleryOrder[] = [];
  @Input() title = 'Invoice';
  open = false;

  get total(): number {
    return this.orders.reduce(
      (sum, order) =>
        sum + (order.billing?.grandTotal ?? order.totalPrice ?? 0),
      0,
    );
  }

  downloadPdf(): void {
    const lines = [
      this.title.toUpperCase(),
      `Generated: ${new Date().toLocaleDateString('en-IN')}`,
      '',
    ];
    this.orders.forEach((order, index) => {
      const item = order.invoiceItem || order.item;
      const bill = order.billing;
      lines.push(`${index + 1}. ${item?.itemName || 'Jewellery piece'}`);
      lines.push(
        `SKU: ${item?.sku || '-'} | ${item?.material || '-'} | ${item?.purity || '-'} | Quantity: ${order.quantity}`,
      );
      if (bill) {
        lines.push(
          `Gross weight: ${bill.grossWeight} g | Metal rate: ${this.money(bill.metalRate)}/g`,
        );
        lines.push(
          `Metal: ${this.money(bill.metalValue)} | Stone: ${this.money(bill.stoneValue)} | Making: ${this.money(bill.makingCharges + bill.vaCharges)}`,
        );
        lines.push(
          `CGST: ${this.money(bill.cgstAmount)} | SGST: ${this.money(bill.sgstAmount)} | Total: ${this.money(bill.grandTotal)}`,
        );
      } else {
        lines.push(`Total: ${this.money(order.totalPrice || 0)}`);
      }
      lines.push('');
    });
    lines.push(`GRAND TOTAL: ${this.money(this.total)}`);

    const pdf = this.createPdf(lines);
    const pdfBuffer = new ArrayBuffer(pdf.byteLength);
    new Uint8Array(pdfBuffer).set(pdf);
    const url = URL.createObjectURL(
      new Blob([pdfBuffer], { type: 'application/pdf' }),
    );
    const link = document.createElement('a');
    link.href = url;
    link.download = `${this.title.toLowerCase().replace(/[^a-z0-9]+/g, '-') || 'gemvault-invoice'}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  private money(value: number): string {
    return `INR ${(value || 0).toFixed(2)}`;
  }

  private createPdf(lines: string[]): Uint8Array {
    const escape = (value: string) =>
      value.replace(/[^\x20-\x7E]/g, '?').replace(/([()\\])/g, '\\$1');
    let y = 800;
    let content = 'BT\n/F1 18 Tf\n';
    lines.slice(0, 45).forEach((line, index) => {
      content += `1 0 0 1 48 ${y} Tm (${escape(line)}) Tj\n`;
      y -= index === 0 ? 30 : 16;
    });
    content += 'ET\n';
    const objects = [
      '<< /Type /Catalog /Pages 2 0 R >>',
      '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
      '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
      `<< /Length ${content.length} >>\nstream\n${content}endstream`,
      '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    ];
    let pdf = '%PDF-1.4\n';
    const offsets = [0];
    objects.forEach((object, index) => {
      offsets.push(pdf.length);
      pdf += `${index + 1} 0 obj\n${object}\nendobj\n`;
    });
    const xref = pdf.length;
    pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
    offsets
      .slice(1)
      .forEach(
        (offset) =>
          (pdf += `${offset.toString().padStart(10, '0')} 00000 n \n`),
      );
    pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
    return new TextEncoder().encode(pdf);
  }
}
