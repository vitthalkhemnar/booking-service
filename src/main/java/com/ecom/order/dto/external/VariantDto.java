package com.ecom.order.dto.external;

import java.math.BigDecimal;

public record VariantDto(
    Long variantId,
    Long productId,
    String sku,
    String color,
    String size,
    BigDecimal price,
    Integer stock,
    String image,
    boolean active
) {}
