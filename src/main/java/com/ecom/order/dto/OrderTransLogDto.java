package com.ecom.order.dto;

import java.time.LocalDateTime;

import com.ecom.order.util.OrderStatus;

public record OrderTransLogDto(
    Long transLogId,
    Long orderId,
    OrderStatus fromState,
    OrderStatus toState,
    String createdBy,
    LocalDateTime createdAt,
    String title,
    String description
) {}
