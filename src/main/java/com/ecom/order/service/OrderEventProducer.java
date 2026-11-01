package com.ecom.order.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecom.order.config.KafkaTopicConfig;
import com.ecom.order.dto.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void publishOrderPlacedEvent(OrderPlacedEvent event) {
		log.info("Publishing OrderPlacedEvent for order: {}", event.order().bookingId());
		kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACED_TOPIC, String.valueOf(event.order().bookingId()), event);
	}

}
