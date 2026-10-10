package com.ecom.order.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecom.order.config.FeignClientConfig;
import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.UserDto;

@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:9090}", configuration = FeignClientConfig.class)
public interface UserClient {

	@GetMapping("/user")
	UserDto getCurrentUser();

	@GetMapping("/user/by-username/{username}")
	UserDto getUserByUsername(@PathVariable("username") String username);

	@GetMapping("/address")
	List<AddressDto> getUserAddresses();

	@GetMapping("/address/{addressId}")
	AddressDto getAddressById(@PathVariable("addressId") Long addressId);
}
