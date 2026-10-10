package com.ecom.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {

	@Bean
	RequestInterceptor requestInterceptor() {
		return requestTemplate -> {
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				String authHeader = request.getHeader("Authorization");
				if (authHeader != null && !authHeader.isBlank()) {
					requestTemplate.header("Authorization", authHeader);
				}
			}
		};
	}
}
