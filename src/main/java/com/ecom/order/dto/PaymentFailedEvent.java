package com.ecom.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedEvent(
		Long orderId,
		String razorpayOrderId,
		String reason
) {}
