package com.ecom.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.client.ProductClient;
import com.ecom.order.client.UserClient;
import com.ecom.order.dto.OrderItemResponse;
import com.ecom.order.dto.OrderPlacedEvent;
import com.ecom.order.dto.OrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.OrderTrackingResponse;
import com.ecom.order.dto.OrderTransLogDto;
import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.ProductDto;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderItem;
import com.ecom.order.entity.OrderTransLog;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.OrderTransLogRepository;
import com.ecom.order.util.CommonUtil;
import com.ecom.order.util.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTransLogRepository ordTransLogRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final OrderEventProducer orderEventProducer;
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {    	
        Order booking = new Order();
        booking.setTotalAmount(request.totalAmount());
        booking.setAddressId(request.addressId());
        booking.setStatus(OrderStatus.CREATED);
        booking.setCreatedAt(LocalDateTime.now());
        
        UserDto currentUser = userClient.getCurrentUser();
        booking.setUserId(currentUser.id());

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

        try {
            OrderTransLog initialLog = new OrderTransLog();
            initialLog.setOrder(savedBooking);
            initialLog.setFromState(null);
            initialLog.setToState(OrderStatus.CREATED);
            initialLog.setCreatedBy(currentUser != null && currentUser.username() != null ? currentUser.username() : "CUSTOMER");
            initialLog.setCreatedAt(savedBooking.getCreatedAt() != null ? savedBooking.getCreatedAt() : LocalDateTime.now());
            ordTransLogRepository.save(initialLog);
        } catch (Exception e) {
            log.warn("Failed to create initial OrderTransLog entry for order #{}: {}", savedBooking.getOrderId(), e.getMessage());
        }

        OrderResponse orderResponse = mapToEnrichedResponse(savedBooking);
        
        // publishing order placed event
        orderEventProducer.publishOrderPlacedEvent(new OrderPlacedEvent(orderResponse, currentUser.email()));

        return orderResponse;
    }

    public OrderResponse getOrderById(Long bookingId) {
    	Order order;
    	if(CommonUtil.isAdmin()) {
    		order = orderRepository.findById(bookingId)
    			.orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));    		
		} else {
			UserDto currentUser = userClient.getCurrentUser();
			order = orderRepository.findByOrderIdAndUserId(bookingId, currentUser.id())
					.orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
		}
        return mapToEnrichedResponse(order);
    }

    public OrderTrackingResponse getOrderTracking(Long orderId) {
        Order order;
        if (CommonUtil.isAdmin()) {
            order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        } else {
            UserDto currentUser = userClient.getCurrentUser();
            order = orderRepository.findByOrderIdAndUserId(orderId, currentUser.id())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        }

        OrderResponse enriched = mapToEnrichedResponse(order);
        boolean isAdmin = CommonUtil.isAdmin();
        List<OrderTransLogDto> logDtos = new ArrayList<>();

        // OrderTransLog Activity Ledger is only exposed to Admin
        if (isAdmin) {
            List<OrderTransLog> logs = ordTransLogRepository.findByOrderOrderByTransLogIdAsc(order);
            if (logs.isEmpty()) {
                logDtos.add(new OrderTransLogDto(
                    1L,
                    order.getOrderId(),
                    null,
                    OrderStatus.CREATED,
                    "CUSTOMER",
                    order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
                    "Order Placed",
                    "Your order has been placed and received by our system."
                ));
                if (order.getStatus() != null && order.getStatus() != OrderStatus.CREATED) {
                    logDtos.add(new OrderTransLogDto(
                        2L,
                        order.getOrderId(),
                        OrderStatus.CREATED,
                        order.getStatus(),
                        "SYSTEM",
                        order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
                        formatStatusTitle(order.getStatus()),
                        formatStatusDescription(order.getStatus())
                    ));
                }
            } else {
                for (OrderTransLog logItem : logs) {
                    logDtos.add(new OrderTransLogDto(
                        logItem.getTransLogId(),
                        order.getOrderId(),
                        logItem.getFromState(),
                        logItem.getToState(),
                        logItem.getCreatedBy() != null ? logItem.getCreatedBy() : "SYSTEM",
                        logItem.getCreatedAt() != null ? logItem.getCreatedAt() : order.getCreatedAt(),
                        formatStatusTitle(logItem.getToState()),
                        formatStatusDescription(logItem.getToState())
                    ));
                }
            }
        }

        return new OrderTrackingResponse(
            order.getOrderId(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getTotalAmount(),
            enriched.user(),
            enriched.address(),
            enriched.items(),
            logDtos
        );
    }

    private String formatStatusTitle(OrderStatus status) {
        if (status == null) return "Status Updated";
        return switch (status) {
            case CREATED -> "Order Placed";
            case CONFIRMED -> "Order Confirmed";
            case PAYMENT_PENDING -> "Payment Pending";
            case PAYMENT_SUCCESS -> "Payment Completed";
            case PAYMENT_FAILED -> "Payment Failed";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Order Cancelled";
            case FAILED -> "Order Failed";
        };
    }

    private String formatStatusDescription(OrderStatus status) {
        if (status == null) return "Order state transition recorded.";
        return switch (status) {
            case CREATED -> "Your order has been placed and received by our system.";
            case CONFIRMED -> "Payment has been verified and seller confirmed your order.";
            case PAYMENT_PENDING -> "Waiting for payment processing and verification.";
            case PAYMENT_SUCCESS -> "Your payment was successfully received.";
            case PAYMENT_FAILED -> "Payment was declined or unsuccessful.";
            case DELIVERED -> "Package has been delivered to your delivery address.";
            case CANCELLED -> "This order has been cancelled.";
            case FAILED -> "Processing encountered an issue and order failed.";
        };
    }

    public List<OrderResponse> getOrders() {
        UserDto currentUser = userClient.getCurrentUser();
        return orderRepository.findByUserId(currentUser.id()).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
                .map(this::mapToEnrichedResponse)
                .toList();
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.reverseOrder()))
                .map(this::mapToEnrichedResponse)
                .toList();
    }

    private OrderResponse mapToEnrichedResponse(Order booking) {
        UserDto user = null;
        AddressDto address = null;

        if (user == null && booking.getUserId() != null) {
            try {
                user = userClient.getUserById(booking.getUserId());
            } catch (Exception e) {
                log.warn("Failed to fetch user by username {} via Feign: {}", booking.getUserId(), e.getMessage());
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
            booking.getUserId(),
            user != null ? user.username() : String.valueOf(booking.getUserId()),
            booking.getTotalAmount(),
            booking.getStatus(),
            booking.getCreatedAt(),
            itemResponses,
            user,
            address
        );
    }
}