package com.ecom.book.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ecom.book.util.OrderStatus;

public record OrderResponse(
    Long bookingId,
    String username,
    OrderStatus status,
    String totalAmount,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {}