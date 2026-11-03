package com.ecom.order.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecom.order.config.FeignClientConfig;
import com.ecom.order.dto.external.ProductDto;
import com.ecom.order.dto.external.VariantDto;

@FeignClient(name = "product-service", url = "${product-service.url:}", configuration = FeignClientConfig.class)
public interface ProductClient {

	@GetMapping("/product/{productId}")
	ProductDto getProductById(@PathVariable("productId") Long productId);

	@GetMapping("/variant/{productId}")
	List<VariantDto> getVariantsByProductId(@PathVariable("productId") Long productId);

	@GetMapping("/variant/item/{variantId}")
	VariantDto getVariantById(@PathVariable("variantId") Long variantId);
}
