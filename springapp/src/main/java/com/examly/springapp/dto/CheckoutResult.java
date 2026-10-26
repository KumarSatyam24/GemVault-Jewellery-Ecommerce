package com.examly.springapp.dto;

import java.util.List;

public record CheckoutResult(Long checkoutId, Double totalPrice, List<ApiDto.OrderView> orders) {}
