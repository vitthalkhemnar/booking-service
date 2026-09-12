package com.ecom.book.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.book.dto.OrderItemResponse;
import com.ecom.book.dto.OrderRequest;
import com.ecom.book.dto.OrderResponse;
import com.ecom.book.entity.Order;
import com.ecom.book.entity.OrderItem;
import com.ecom.book.repository.OrderRepository;
import com.ecom.book.util.CommonUtil;
import com.ecom.book.util.OrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {    	
        Order booking = new Order();
        booking.setTotalAmount(request.totalAmount());
        booking.setAddressId(request.addressId());
        booking.setStatus(OrderStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        
		String username = CommonUtil.getCurrentUsername();
        booking.setUsername(username);

        List<OrderItem> items = request.items().stream().map(itemDto -> {
            OrderItem item = new OrderItem();
            item.setProductId(itemDto.productId());
            item.setVariantId(itemDto.variantId());
            item.setProductName(itemDto.productName());
            item.setSize(itemDto.size());
            item.setColor(itemDto.color());
            item.setQuantity(itemDto.quantity());
            item.setPriceAtBooking(itemDto.priceAtBooking());
            return item;
        }).toList();

        booking.getItems().addAll(items);
        
        Order savedBooking = orderRepository.save(booking);

        return mapToResponse(savedBooking);
    }

    public OrderResponse getOrderById(Long bookingId) {
        return orderRepository.findById(bookingId)
        		.map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
    }

    public List<OrderResponse> getOrdersByUsername() {
		String username = CommonUtil.getCurrentUsername();
        return orderRepository.findByUsername(username).stream()
        		.sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
        		.map(this::mapToResponse).toList();
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
        		.sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
        		.map(this::mapToResponse).toList();
    }
    
    private OrderResponse mapToResponse(Order booking) {
        List<OrderItemResponse> itemResponses = booking.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getOrderItemId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductName(),
                item.getSize(),
                item.getColor(),
                item.getQuantity(),
                item.getPriceAtBooking()
            )).toList();

        return new OrderResponse(
            booking.getOrderId(),
            booking.getAddressId(),
            booking.getUsername(),
            booking.getTotalAmount(),
            booking.getStatus(),
            booking.getCreatedAt(),
            itemResponses
        );
    }
}