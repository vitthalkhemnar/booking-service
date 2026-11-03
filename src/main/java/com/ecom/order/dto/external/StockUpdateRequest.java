package com.ecom.order.dto.external;

public record StockUpdateRequest(
    Long variantId,
    Integer quantity
) {}
