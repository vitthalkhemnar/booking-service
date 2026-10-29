package com.ecom.order.dto;

import com.ecom.order.util.OrderStatus;

public record OrderStateChangeRequest(
	String orderId,
    OrderStatus from,
    OrderStatus to
) {}