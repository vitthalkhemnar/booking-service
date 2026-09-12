package com.ecom.book.dto;

import java.util.List;

public record OrderRequest(
    String totalAmount,
    Long addressId,
    List<OrderItemRequest> items
) {}