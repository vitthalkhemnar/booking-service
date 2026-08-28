package com.ecom.book.dto;

public record OrderItemRequest(
    String productId,
    String variantId,
    String productName,
    String size,
    String color,
    String quantity,
    String priceAtBooking
) {}