package com.ecom.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.client.ProductClient;
import com.ecom.order.client.UserClient;
import com.ecom.order.dto.OrderItemResponse;
import com.ecom.order.dto.OrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.ProductDto;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderItem;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.util.CommonUtil;
import com.ecom.order.util.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    
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

        return mapToEnrichedResponse(savedBooking);
    }

    public OrderResponse getOrderById(Long bookingId) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        return mapToEnrichedResponse(order);
    }

    public List<OrderResponse> getOrdersByUsername() {
        String username = CommonUtil.getCurrentUsername();
        return orderRepository.findByUsername(username).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
                .map(this::mapToResponse)
                .toList();
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
                .map(this::mapToResponse)
                .toList();
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

    private OrderResponse mapToEnrichedResponse(Order booking) {
        UserDto user = null;
        AddressDto address = null;

        // Fetch User details via OpenFeign
        try {
            user = userClient.getCurrentUser();
        } catch (Exception e) {
            log.warn("Failed to fetch current user via Feign: {}", e.getMessage());
        }
        if (user == null && booking.getUsername() != null && !booking.getUsername().isBlank()) {
            try {
                user = userClient.getUserByUsername(booking.getUsername());
            } catch (Exception e) {
                log.warn("Failed to fetch user by username {} via Feign: {}", booking.getUsername(), e.getMessage());
            }
        }

        // Fetch Address details via OpenFeign
        if (booking.getAddressId() != null) {
            try {
                address = userClient.getAddressById(booking.getAddressId());
            } catch (Exception e) {
                log.warn("Failed to fetch address by id {} via Feign: {}", booking.getAddressId(), e.getMessage());
            }

            if (address == null) {
                try {
                    List<AddressDto> addresses = userClient.getUserAddresses();
                    if (addresses != null) {
                        address = addresses.stream()
                            .filter(a -> a.addressId() != null && a.addressId().equals(booking.getAddressId()))
                            .findFirst()
                            .orElse(null);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch address list via Feign: {}", e.getMessage());
                }
            }
        }

        // Fetch Product details (brand, image) via OpenFeign for each item
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : booking.getItems()) {
            String brand = null;
            String imageUrl = null;

            if (item.getProductId() != null && !item.getProductId().isBlank()) {
                try {
                    Long pId = Long.parseLong(item.getProductId());
                    ProductDto product = productClient.getProductById(pId);
                    if (product != null) {
                        brand = product.brand();
                        if (product.images() != null && !product.images().isEmpty()) {
                            imageUrl = product.images().get(0);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch product {} via Feign: {}", item.getProductId(), e.getMessage());
                }
            }

            itemResponses.add(new OrderItemResponse(
                item.getOrderItemId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductName(),
                item.getSize(),
                item.getColor(),
                item.getQuantity(),
                item.getPriceAtBooking(),
                brand,
                imageUrl
            ));
        }

        return new OrderResponse(
            booking.getOrderId(),
            booking.getAddressId(),
            booking.getUsername(),
            booking.getTotalAmount(),
            booking.getStatus(),
            booking.getCreatedAt(),
            itemResponses,
            user,
            address
        );
    }
}