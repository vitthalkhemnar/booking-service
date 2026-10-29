package com.ecom.order.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.order.dto.OrderStateChangeRequest;
import com.ecom.order.service.OrderStateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/state")
@RequiredArgsConstructor
public class OrderStateController {
	
	private final OrderStateService orderStateService;

	@PostMapping("/change-status")
	public void changeStatus(@RequestBody OrderStateChangeRequest req) {
		orderStateService.changeStatus(req);		
	}

}
