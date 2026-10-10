package com.ecom.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.util.OrderStatus;

public record OrderResponse(
    Long bookingId,
    Long addressId,
    String username,
    String totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    List<OrderItemResponse> items,
    UserDto user,
    AddressDto address
) {
    public OrderResponse(
        Long bookingId,
        Long addressId,
        String username,
        String totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
    ) {
        this(bookingId, addressId, username, totalAmount, status, createdAt, items, null, null);
    }
}