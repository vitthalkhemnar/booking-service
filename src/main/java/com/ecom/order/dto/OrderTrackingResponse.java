package com.ecom.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.util.OrderStatus;

public record OrderTrackingResponse(
    Long orderId,
    OrderStatus currentStatus,
    LocalDateTime createdAt,
    String totalAmount,
    UserDto user,
    AddressDto address,
    List<OrderItemResponse> items,
    List<OrderTransLogDto> logs
) {}
