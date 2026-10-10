package com.ecom.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecom.order.config.FeignClientConfig;
import com.ecom.order.dto.external.ProductDto;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:9091}", configuration = FeignClientConfig.class)
public interface ProductClient {

	@GetMapping("/product/{productId}")
	ProductDto getProductById(@PathVariable("productId") Long productId);
}
