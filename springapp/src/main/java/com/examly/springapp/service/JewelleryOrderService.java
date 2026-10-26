package com.examly.springapp.service;

import com.examly.springapp.model.JewelleryOrder;
import java.util.*;

public interface JewelleryOrderService {
  List<JewelleryOrder> getAllOrders();

  List<JewelleryOrder> getOrdersByUserId(Long userId);

  Optional<JewelleryOrder> getOrderById(Long orderId);

  JewelleryOrder addOrder(JewelleryOrder order);

  JewelleryOrder updateOrder(Long orderId, JewelleryOrder updatedOrder);

  boolean deleteOrder(Long orderId);

  JewelleryOrder cancelOrder(Long orderId, String reason);
}
