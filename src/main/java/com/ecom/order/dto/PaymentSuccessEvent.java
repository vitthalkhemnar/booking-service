package com.ecom.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentSuccessEvent(
		Long orderId,
		String razorpayOrderId,
		String razorpayPaymentId,
		String status
) {}
