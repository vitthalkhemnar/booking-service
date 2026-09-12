package com.ecom.book.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ecom.book.util.OrderStatus;

public record OrderResponse(
    Long bookingId,
    Long addressId,
    String username,
    String totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {}