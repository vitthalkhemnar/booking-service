package com.ecom.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.order.dto.OrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.OrderTrackingResponse;
import com.ecom.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    
    @Tag(name = "Customer Orders", description = "Endpoints for placing orders and viewing personal order history")
    @Operation(
        summary = "Create a new order",
        description = "Creates a new customer order for the specified items, delivery address, and payment method."
    )
    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        OrderResponse createdBooking = orderService.createOrder(request);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @Tag(name = "Customer Orders", description = "Endpoints for placing orders and viewing personal order history")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves order details by unique order identifier."
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Unique ID of the order", required = true)
            @PathVariable Long orderId) {
        OrderResponse booking = orderService.getOrderById(orderId);
        return ResponseEntity.ok(booking);
    }

    @Tag(name = "Customer Orders", description = "Endpoints for placing orders and viewing personal order history")
    @Operation(
        summary = "Get order tracking flow",
        description = "Retrieves live tracking status and chronological state transitions from OrderTransLog for an order."
    )
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<OrderTrackingResponse> getOrderTracking(
            @Parameter(description = "Unique ID of the order", required = true)
            @PathVariable Long orderId) {
        OrderTrackingResponse tracking = orderService.getOrderTracking(orderId);
        return ResponseEntity.ok(tracking);
    }

    @Tag(name = "Customer Orders", description = "Endpoints for placing orders and viewing personal order history")
    @Operation(
        summary = "Get current user orders",
        description = "Retrieves all past orders placed by the currently authenticated customer."
    )
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders() {
        List<OrderResponse> bookings = orderService.getOrders();
        return ResponseEntity.ok(bookings);
    }
    
    @Tag(name = "Order Administration", description = "Administrative endpoints for managing and inspecting platform orders")
    @Operation(
        summary = "Get all platform orders",
        description = "Retrieves all orders placed across the entire platform. Requires ROLE_ADMIN authority."
    )
    @GetMapping("/all-orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> bookings = orderService.getAllOrders();
        return ResponseEntity.ok(bookings);
    }
}