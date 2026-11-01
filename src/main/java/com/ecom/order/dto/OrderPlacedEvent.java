package com.ecom.order.dto;

public record OrderPlacedEvent(
	    OrderResponse order,
	    String email
) {
}
