package com.ecom.order.dto;

public record OrderItemResponse(
    Long bookingItemId,
    String productId,
    String variantId,
    String productName,
    String size,
    String color,
    String quantity,
    String priceAtBooking,
    String brand,
    String imageUrl
) {
    public OrderItemResponse(
        Long bookingItemId,
        String productId,
        String variantId,
        String productName,
        String size,
        String color,
        String quantity,
        String priceAtBooking
    ) {
        this(bookingItemId, productId, variantId, productName, size, color, quantity, priceAtBooking, null, null);
    }
}