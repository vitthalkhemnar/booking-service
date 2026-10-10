package com.ecom.order.dto.external;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
    Long id,
    String productCode,
    String productName,
    String description,
    String category,
    String subcategory,
    String brand,
    BigDecimal price,
    BigDecimal discount,
    List<String> images
) {}
