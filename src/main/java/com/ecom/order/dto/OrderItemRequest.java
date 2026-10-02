package com.ecom.order.dto;

public record OrderItemRequest(
    String productId,
    String variantId,
    String productName,
    String size,
    String color,
    String quantity,
    String priceAtBooking
) {}