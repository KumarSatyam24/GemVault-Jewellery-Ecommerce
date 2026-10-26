package com.examly.springapp.service;

import com.examly.springapp.model.JewelleryItem;

public final class BillingCalculator {
  private BillingCalculator() {
  }

  public record Breakdown(double grossWeight, double stoneWeight, double netMetalWeight,
      double metalRate, double metalValue, double stoneValue, double makingCharges,
      double vaCharges, double discountAmount, double taxableAmount, double cgstRate,
      double cgstAmount, double sgstRate, double sgstAmount, double roundOff, double grandTotal) {
  }

  public static Breakdown forItem(JewelleryItem item) {
    double gross = number(item.getWeightInGrams());
    double stoneWeight = number(item.getStoneWeightInGrams());
    double net = Math.max(0, gross - stoneWeight);
    double rate = number(item.getPricePerGram());
    double metal = round(net * rate);
    double stone = number(item.getStoneValue());
    double making = number(item.getMakingCharges());
    double va = number(item.getVaCharges());
    double discount = number(item.getDiscountAmount());
    double taxable = round(Math.max(0, metal + stone + making + va - discount));
    double cgstRate = number(item.getCgstRate());
    double sgstRate = number(item.getSgstRate());
    double cgst = round(taxable * cgstRate / 100d);
    double sgst = round(taxable * sgstRate / 100d);
    double roundOff = number(item.getRoundOff());
    return new Breakdown(gross, stoneWeight, net, rate, metal, stone, making, va, discount,
        taxable, cgstRate, cgst, sgstRate, sgst, roundOff,
        round(taxable + cgst + sgst + roundOff));
  }

  public static double round(double value) {
    return Math.round(value * 100d) / 100d;
  }

  private static double number(Double value) {
    return value == null ? 0d : value;
  }
}
