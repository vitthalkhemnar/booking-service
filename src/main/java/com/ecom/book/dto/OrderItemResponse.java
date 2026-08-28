package com.ecom.book.dto;

public record OrderItemResponse(
    Long bookingItemId,
    String productId,
    String variantId,
    String productName,
    String size,
    String color,
    String quantity,
    String priceAtBooking
) {}