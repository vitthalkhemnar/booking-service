package com.ecom.order.config;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {

	private static final String SECRET_STRING = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
	private static volatile String cachedSystemToken;
	private static volatile long tokenExpiryTime = 0;

	@Bean
	RequestInterceptor requestInterceptor() {
		return requestTemplate -> {
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				String authHeader = request.getHeader("Authorization");
				if (authHeader != null && !authHeader.isBlank()) {
					requestTemplate.header("Authorization", authHeader);
					return;
				}
			}

			// Background / Kafka listener thread without incoming HTTP request context
			// Provide internal system service-to-service admin token
			requestTemplate.header("Authorization", "Bearer " + getSystemToken());
		};
	}

	private String getSystemToken() {
		long now = System.currentTimeMillis();
		if (cachedSystemToken == null || now >= tokenExpiryTime) {
			synchronized (FeignClientConfig.class) {
				if (cachedSystemToken == null || now >= tokenExpiryTime) {
					tokenExpiryTime = now + (3600 * 1000); // 1 hour
					cachedSystemToken = Jwts.builder()
							.subject("order-service")
							.claims(Map.of("role", List.of("ROLE_ADMIN", "ADMIN")))
							.issuedAt(new Date(now))
							.expiration(new Date(tokenExpiryTime))
							.signWith(Keys.hmacShaKeyFor(SECRET_STRING.getBytes()))
							.compact();
				}
			}
		}
		return cachedSystemToken;
	}
}
