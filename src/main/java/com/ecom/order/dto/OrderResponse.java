package com.ecom.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ecom.order.util.OrderStatus;

public record OrderResponse(
    Long bookingId,
    Long addressId,
    String username,
    String totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {}