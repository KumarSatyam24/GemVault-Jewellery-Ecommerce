package com.examly.springapp.controller;

import com.examly.springapp.service.MetalRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metal-rates")
public class MetalRateController {
  private final MetalRateService rates;

  public MetalRateController(MetalRateService rates) {
    this.rates = rates;
  }

  @GetMapping
  public MetalRateService.Quote quote(@RequestParam String material, @RequestParam(required = false) String purity,
      @RequestParam(defaultValue = "false") boolean refresh) {
    return rates.quote(material, purity, refresh);
  }
}
