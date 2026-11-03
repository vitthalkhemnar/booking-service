package com.ecom.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.order.dto.OrderStateChangeRequest;
import com.ecom.order.service.OrderStateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/state")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order State Management", description = "Endpoints for managing order workflow and status transitions")
public class OrderStateController {
	
	private final OrderStateService orderStateService;

	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
		summary = "Change order status",
		description = "Transition an existing order to a new status (e.g. CONFIRMED, DELIVERED, CANCELLED). Requires ADMIN role."
	)
	@PostMapping("/change-status")
	public ResponseEntity<Void> changeStatus(@RequestBody OrderStateChangeRequest req) {
		orderStateService.changeStatus(req);
		return ResponseEntity.ok().build();
	}

}
