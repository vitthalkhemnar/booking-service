package com.ecom.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecom.order.client.ProductClient;
import com.ecom.order.client.UserClient;
import com.ecom.order.dto.OrderItemRequest;
import com.ecom.order.dto.OrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.external.ProductDto;
import com.ecom.order.dto.external.UserDto;
import com.ecom.order.dto.external.VariantDto;
import com.ecom.order.entity.Order;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.OrderTransLogRepository;

@ExtendWith(MockitoExtension.class)
class OrderServicePriceVerificationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTransLogRepository ordTransLogRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(100L, "testuser", "Test", "User", "test@example.com", "9876543210");
        when(userClient.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("Should detect and reject client price tampering when totalAmount is manipulated to 1.00")
    void shouldRejectPriceTampering() {
        // Product costs 1299.00
        ProductDto product = new ProductDto(
                1001L, "CLO-TSH-1001", "Allen Solly T-Shirt", "Desc",
                "Clothing", "T-Shirt", "Allen Solly",
                new BigDecimal("1299.00"), BigDecimal.ZERO, List.of("img1.jpg")
        );
        VariantDto variant = new VariantDto(
                100101L, 1001L, "SKU-100101", "Navy Blue", "M",
                new BigDecimal("1299.00"), 50, "img1.jpg", true
        );

        when(productClient.getProductById(1001L)).thenReturn(product);
        when(productClient.getVariantById(100101L)).thenReturn(variant);

        // Attacker attempts to buy 2 units (worth 2598.00) for only 1.00
        OrderItemRequest tamperedItem = new OrderItemRequest(
                "1001", "100101", "Allen Solly T-Shirt", "M", "Navy Blue", "2", "0.50"
        );
        OrderRequest tamperedOrder = new OrderRequest("1.00", 1L, List.of(tamperedItem));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(tamperedOrder);
        });

        assertTrue(exception.getMessage().contains("Price mismatch detected"));
        assertTrue(exception.getMessage().contains("2598.00"));
        assertTrue(exception.getMessage().contains("1.00"));
    }

    @Test
    @DisplayName("Should correctly recalculate line items and accept legitimate order")
    void shouldCalculateAndAcceptValidOrder() {
        ProductDto product = new ProductDto(
                1001L, "CLO-TSH-1001", "Allen Solly T-Shirt", "Desc",
                "Clothing", "T-Shirt", "Allen Solly",
                new BigDecimal("1299.00"), BigDecimal.ZERO, List.of("img1.jpg")
        );
        VariantDto variant = new VariantDto(
                100101L, 1001L, "SKU-100101", "Navy Blue", "M",
                new BigDecimal("1299.00"), 50, "img1.jpg", true
        );

        when(productClient.getProductById(1001L)).thenReturn(product);
        when(productClient.getVariantById(100101L)).thenReturn(variant);

        OrderItemRequest item = new OrderItemRequest(
                "1001", "100101", "Allen Solly T-Shirt", "M", "Navy Blue", "2", "1299.00"
        );
        OrderRequest validOrder = new OrderRequest("2598.00", 1L, List.of(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(5001L);
            return o;
        });

        OrderResponse response = orderService.createOrder(validOrder);

        assertNotNull(response);
        assertEquals("2598.00", response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals("1299.00", response.items().get(0).priceAtBooking());
        assertEquals("2", response.items().get(0).quantity());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject order when quantity is negative or zero")
    void shouldRejectInvalidQuantity() {
        OrderItemRequest invalidItem = new OrderItemRequest(
                "1001", "100101", "Allen Solly T-Shirt", "M", "Navy Blue", "0", "1299.00"
        );
        OrderRequest order = new OrderRequest("0.00", 1L, List.of(invalidItem));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(order);
        });

        assertTrue(ex.getMessage().contains("greater than zero"));
    }

    @Test
    @DisplayName("Should reject order when variant does not belong to specified product")
    void shouldRejectMismatchedVariant() {
        ProductDto product = new ProductDto(
                1001L, "CLO-TSH-1001", "Product A", "Desc",
                "Clothing", "T-Shirt", "Brand",
                new BigDecimal("100.00"), BigDecimal.ZERO, List.of()
        );
        // Variant belongs to product 9999L instead of 1001L
        VariantDto variant = new VariantDto(
                5555L, 9999L, "SKU-MISMATCH", "Black", "L",
                new BigDecimal("100.00"), 10, null, true
        );

        when(productClient.getProductById(1001L)).thenReturn(product);
        when(productClient.getVariantById(5555L)).thenReturn(variant);

        OrderItemRequest item = new OrderItemRequest(
                "1001", "5555", "Product A", "L", "Black", "1", "100.00"
        );
        OrderRequest order = new OrderRequest("100.00", 1L, List.of(item));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(order);
        });

        assertTrue(ex.getMessage().contains("does not belong to Product ID"));
    }
}
