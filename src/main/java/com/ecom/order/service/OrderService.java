package com.ecom.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.client.ProductClient;
import com.ecom.order.client.UserClient;
import com.ecom.order.dto.OrderItemRequest;
import com.ecom.order.dto.OrderItemResponse;
import com.ecom.order.dto.OrderPlacedEvent;
import com.ecom.order.dto.OrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.OrderTrackingResponse;
import com.ecom.order.dto.OrderTransLogDto;
import com.ecom.order.dto.external.AddressDto;
import com.ecom.order.dto.external.ProductDto;
import com.ecom.order.dto.external.StockUpdateRequest;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.dto.external.VariantDto;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderItem;
import com.ecom.order.entity.OrderTransLog;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.OrderTransLogRepository;
import com.ecom.order.util.CommonUtil;
import com.ecom.order.util.OrderStatus;

import feign.FeignException;
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
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }
        if (request.addressId() == null) {
            throw new IllegalArgumentException("Delivery address is required.");
        }

        UserDto currentUser = userClient.getCurrentUser();
        if (currentUser == null || currentUser.id() == null) {
            throw new IllegalStateException("Failed to resolve current authenticated user.");
        }

        Order booking = new Order();
        booking.setAddressId(request.addressId());
        booking.setStatus(OrderStatus.CREATED);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUserId(currentUser.id());

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        Map<Long, Integer> variantQuantities = new LinkedHashMap<>();
        Map<Long, VariantDto> resolvedVariants = new HashMap<>();

        for (OrderItemRequest itemDto : request.items()) {
            if (itemDto.productId() == null || itemDto.productId().isBlank()) {
                throw new IllegalArgumentException("Product ID is required for each item.");
            }

            int quantity;
            try {
                quantity = Integer.parseInt(itemDto.quantity().trim());
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Item quantity must be greater than zero.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity format for product: " + itemDto.productId());
            }

            Long productId;
            try {
                productId = Long.parseLong(itemDto.productId().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid product ID: " + itemDto.productId());
            }

            ProductDto product = productClient.getProductById(productId);
            if (product == null) {
                throw new IllegalArgumentException("Product not found with ID: " + productId);
            }

            BigDecimal unitPrice = null;
            Long variantId = null;

            if (itemDto.variantId() != null && !itemDto.variantId().isBlank()) {
                try {
                    variantId = Long.parseLong(itemDto.variantId().trim());
                    VariantDto variant = null;
                    try {
                        variant = productClient.getVariantById(variantId);
                    } catch (Exception e) {
                        log.debug("Direct variant lookup by id {} failed: {}", variantId, e.getMessage());
                    }

                    if (variant == null) {
                        try {
                            List<VariantDto> variants = productClient.getVariantsByProductId(productId);
                            if (variants != null) {
                                final Long targetVariantId = variantId;
                                variant = variants.stream()
                                        .filter(v -> targetVariantId.equals(v.variantId()))
                                        .findFirst()
                                        .orElse(null);
                            }
                        } catch (Exception e) {
                            log.warn("Variant lookup by productId {} failed: {}", productId, e.getMessage());
                        }
                    }

                    if (variant != null) {
                        if (!productId.equals(variant.productId())) {
                            throw new IllegalArgumentException(
                                    "Variant ID " + variantId + " does not belong to Product ID " + productId
                            );
                        }
                        if (!variant.active()) {
                            throw new IllegalArgumentException(
                                    "Variant " + (variant.sku() != null ? variant.sku() : variantId) + " is not currently active."
                            );
                        }
                        unitPrice = variant.price();
                        resolvedVariants.put(variantId, variant);
                        variantQuantities.merge(variantId, quantity, Integer::sum);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Fallback to product price if no variant price was resolved
            if (unitPrice == null) {
                unitPrice = product.price();
            }

            if (unitPrice == null) {
                throw new IllegalStateException("Authoritative price could not be determined for product ID: " + productId);
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            calculatedTotal = calculatedTotal.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setProductId(String.valueOf(productId));
            item.setVariantId(variantId != null ? String.valueOf(variantId) : (itemDto.variantId() != null ? itemDto.variantId().trim() : ""));
            item.setProductName(product.productName() != null ? product.productName() : itemDto.productName());
            item.setSize(itemDto.size());
            item.setColor(itemDto.color());
            item.setQuantity(String.valueOf(quantity));
            item.setPriceAtBooking(unitPrice.setScale(2, RoundingMode.HALF_UP).toString());
            items.add(item);
        }

        // 1. INVENTORY VERIFICATION: Check available stock across all requested variants
        for (Map.Entry<Long, Integer> entry : variantQuantities.entrySet()) {
            Long vId = entry.getKey();
            int requestedQty = entry.getValue();
            VariantDto variant = resolvedVariants.get(vId);

            int availableStock = (variant != null && variant.stock() != null) ? variant.stock() : 0;
            if (availableStock <= 0) {
                throw new IllegalArgumentException(
                        String.format("Product variant %s is currently out of stock.",
                                (variant != null && variant.sku() != null) ? variant.sku() : String.valueOf(vId))
                );
            }
            if (requestedQty > availableStock) {
                throw new IllegalArgumentException(
                        String.format("Insufficient stock for variant %s. Requested: %d, Available: %d",
                                (variant != null && variant.sku() != null) ? variant.sku() : String.valueOf(vId),
                                requestedQty,
                                availableStock)
                );
            }
        }

        BigDecimal serverCalculatedTotal = calculatedTotal.setScale(2, RoundingMode.HALF_UP);

        // 2. PRICE INTEGRITY VERIFICATION: Verify client-submitted total against authoritative server-calculated total
        if (request.totalAmount() != null && !request.totalAmount().isBlank()) {
            try {
                BigDecimal clientTotal = new BigDecimal(request.totalAmount().trim()).setScale(2, RoundingMode.HALF_UP);
                if (clientTotal.compareTo(serverCalculatedTotal) != 0) {
                    log.warn("Price mismatch rejected for user ID {}: client sent ₹{}, server computed ₹{}",
                            currentUser.id(), clientTotal, serverCalculatedTotal);
                    throw new IllegalArgumentException(
                            String.format("Price mismatch detected. Expected order total: ₹%s, but received: ₹%s. Please refresh your cart.",
                                    serverCalculatedTotal, clientTotal)
                    );
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid totalAmount format: " + request.totalAmount());
            }
        }

        // 3. INVENTORY DEDUCTION: Atomically deduct stock in product-service before finalizing order
        List<StockUpdateRequest> stockDeductionRequests = variantQuantities.entrySet().stream()
                .map(e -> new StockUpdateRequest(e.getKey(), e.getValue()))
                .toList();

        if (!stockDeductionRequests.isEmpty()) {
            try {
                productClient.deductStock(stockDeductionRequests);
            } catch (FeignException e) {
                log.error("Stock deduction rejected by product-service: status={}, body={}", e.status(), e.contentUTF8());
                throw new IllegalArgumentException("Stock reservation failed: " + extractFeignMessage(e));
            } catch (Exception e) {
                log.error("Unexpected error during stock deduction: {}", e.getMessage());
                throw new IllegalStateException("Failed to reserve product inventory: " + e.getMessage());
            }
        }

        // 4. PERSISTENCE: Save order with rollback compensation if database save fails
        Order savedBooking;
        try {
            booking.setTotalAmount(serverCalculatedTotal.toString());
            booking.getItems().addAll(items);
            savedBooking = orderRepository.save(booking);
        } catch (Exception e) {
            log.error("Failed to persist order in database, restoring reserved stock: {}", e.getMessage());
            if (!stockDeductionRequests.isEmpty()) {
                try {
                    productClient.restoreStock(stockDeductionRequests);
                } catch (Exception restoreEx) {
                    log.error("Failed to restore stock after order save failure: {}", restoreEx.getMessage());
                }
            }
            throw e;
        }

        try {
            OrderTransLog initialLog = new OrderTransLog();
            initialLog.setOrder(savedBooking);
            initialLog.setFromState(null);
            initialLog.setToState(OrderStatus.CREATED);
            initialLog.setCreatedBy(currentUser.username() != null ? currentUser.username() : "CUSTOMER");
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

    private String extractFeignMessage(FeignException e) {
        String content = e.contentUTF8();
        if (content != null && content.contains("\"message\":")) {
            try {
                int start = content.indexOf("\"message\":\"") + 11;
                int end = content.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return content.substring(start, end);
                }
            } catch (Exception ignored) {}
        }
        return (e.getMessage() != null) ? e.getMessage() : "Insufficient inventory or item unavailable.";
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
        List<OrderTransLogDto> logDtos = ordTransLogRepository.findByOrderOrderByTransLogIdAsc(order).stream()
            .map(logItem -> new OrderTransLogDto(
                logItem.getTransLogId(),
                order.getOrderId(),
                logItem.getFromState(),
                logItem.getToState(),
                logItem.getCreatedBy() != null ? logItem.getCreatedBy() : "SYSTEM",
                logItem.getCreatedAt() != null ? logItem.getCreatedAt() : order.getCreatedAt(),
                formatStatusTitle(logItem.getToState()),
                formatStatusDescription(logItem.getToState())
            ))
            .toList();

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
