package com.ecom.order.dto;

import java.util.List;

public record OrderRequest(
    String totalAmount,
    Long addressId,
    List<OrderItemRequest> items
) {}