package com.ecom.book.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.book.dto.OrderRequest;
import com.ecom.book.dto.OrderResponse;
import com.ecom.book.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        OrderResponse createdBooking = orderService.createOrder(request);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        OrderResponse booking = orderService.getOrderById(orderId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    public ResponseEntity<?> getOrdersByUsername() {
        List<OrderResponse> bookings = orderService.getOrdersByUsername();
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/all-orders")
    public ResponseEntity<?> getAllOrders() {
        List<OrderResponse> bookings = orderService.getAllOrders();
        return ResponseEntity.ok(bookings);
    }
}