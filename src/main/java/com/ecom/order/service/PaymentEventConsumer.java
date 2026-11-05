package com.ecom.order.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecom.order.dto.PaymentFailedEvent;
import com.ecom.order.dto.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final OrderService orderService;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = "payment-success-topic", groupId = "order-service-group")
	public void handlePaymentSuccess(String payload) {
		try {
			PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
			log.info("Received PaymentSuccessEvent via Kafka for order: {}", event.orderId());
			orderService.confirmOrder(event.orderId());
		} catch (Exception e) {
			log.error("Failed to process PaymentSuccessEvent: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(topics = "payment-failed-topic", groupId = "order-service-group")
	public void handlePaymentFailed(String payload) {
		try {
			PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
			log.info("Received PaymentFailedEvent via Kafka for order: {}", event.orderId());
			orderService.rollbackOrder(event.orderId(), "Payment failed: " + event.reason());
		} catch (Exception e) {
			log.error("Failed to process PaymentFailedEvent: {}", e.getMessage(), e);
		}
	}
}
