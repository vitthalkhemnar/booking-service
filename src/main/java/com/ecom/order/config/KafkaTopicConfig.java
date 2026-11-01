package com.ecom.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
	
	public static final String ORDER_PLACED_TOPIC = "order-placed-topic";
	
	@Bean
	NewTopic orderPlacedTopic() {
		return TopicBuilder.name(ORDER_PLACED_TOPIC)
				.partitions(1)
				.replicas(1)
				.build();
	}

}
