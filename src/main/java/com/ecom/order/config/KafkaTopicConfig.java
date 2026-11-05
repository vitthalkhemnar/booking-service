package com.ecom.order.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaTopicConfig {
	
	public static final String ORDER_PLACED_TOPIC = "order-placed-topic";
	public static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";
	public static final String PAYMENT_FAILED_TOPIC = "payment-failed-topic";
	
	@Bean
	NewTopic orderPlacedTopic() {
		return TopicBuilder.name(ORDER_PLACED_TOPIC)
				.partitions(1)
				.replicas(1)
				.build();
	}

	@Bean
	NewTopic paymentSuccessTopic() {
		return TopicBuilder.name(PAYMENT_SUCCESS_TOPIC)
				.partitions(1)
				.replicas(1)
				.build();
	}

	@Bean
	NewTopic paymentFailedTopic() {
		return TopicBuilder.name(PAYMENT_FAILED_TOPIC)
				.partitions(1)
				.replicas(1)
				.build();
	}

	@Bean
	KafkaAdmin kafkaAdmin(KafkaProperties properties) {
		Map<String, Object> configs = properties.buildAdminProperties(null);
		applyCaCert(configs::putAll);
		return new KafkaAdmin(configs);
	}

	@Bean
	DefaultKafkaProducerFactoryCustomizer kafkaProducerCustomizer() {
		return factory -> applyCaCert(factory::updateConfigs);
	}

	@Bean
	DefaultKafkaConsumerFactoryCustomizer kafkaConsumerCustomizer() {
		return factory -> applyCaCert(factory::updateConfigs);
	}

	private void applyCaCert(Consumer<Map<String, Object>> configUpdater) {
		try {
			ClassPathResource resource = new ClassPathResource("ca.pem");
			if (resource.exists()) {
				File tempCertFile = File.createTempFile("kafka-ca-", ".pem");
				tempCertFile.deleteOnExit();
				try (InputStream in = resource.getInputStream();
					 OutputStream out = new FileOutputStream(tempCertFile)) {
					in.transferTo(out);
				}
				log.info("Kafka CA cert extracted from classpath to: {}", tempCertFile.getAbsolutePath());
				configUpdater.accept(Map.of(
					SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tempCertFile.getAbsolutePath(),
					SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM"
				));
			}
		} catch (Exception e) {
			log.warn("Could not configure Kafka CA cert: {}", e.getMessage());
		}
	}
}
