package com.ecom.order.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.client.ProductClient;
import com.ecom.order.dto.OrderStateChangeRequest;
import com.ecom.order.dto.external.StockUpdateRequest;
import com.ecom.order.entity.Order;
import com.ecom.order.entity.OrderTransLog;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.OrderTransLogRepository;
import com.ecom.order.util.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateService {
	
	private final OrderRepository orderRepository;
	private final OrderTransLogRepository ordTransLogRepository;
	private final ProductClient productClient;
	
	@Transactional
	public void changeStatus(OrderStateChangeRequest req) {
		Long orderId = Long.valueOf(req.orderId());
		Optional<Order> orderOpt = orderRepository.findById(orderId);
		
		if(orderOpt.isEmpty()) return;
		Order order = orderOpt.get();
		
		OrderStatus currentStatus = order.getStatus();
		OrderStatus toState = req.to();
		
		if (ordTransLogRepository.existsByOrderAndFromStateAndToState(order, currentStatus, toState)) {
			return;
		}

		// Restore inventory stock if order is transitioning to CANCELLED
		if (toState == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED && order.getItems() != null) {
			List<StockUpdateRequest> restoreRequests = order.getItems().stream()
					.filter(item -> item.getVariantId() != null && !item.getVariantId().isBlank())
					.map(item -> {
						try {
							Long vId = Long.parseLong(item.getVariantId().trim());
							int qty = Integer.parseInt(item.getQuantity().trim());
							return new StockUpdateRequest(vId, qty);
						} catch (NumberFormatException ignored) {
							return null;
						}
					})
					.filter(Objects::nonNull)
					.toList();

			if (!restoreRequests.isEmpty()) {
				try {
					productClient.restoreStock(restoreRequests);
					log.info("Successfully restored inventory for cancelled order #{}", order.getOrderId());
				} catch (Exception e) {
					log.error("Failed to restore inventory for cancelled order #{}: {}", order.getOrderId(), e.getMessage());
				}
			}
		}
		
		order.setStatus(toState);
		orderRepository.save(order);
		
		OrderTransLog orderTransLog = new OrderTransLog();
		orderTransLog.setOrder(order);
		orderTransLog.setFromState(currentStatus);
		orderTransLog.setToState(toState);
		
		String actor = "SYSTEM";
		try {
			if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
				String authName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
				if (authName != null && !authName.equalsIgnoreCase("anonymousUser") && !authName.isBlank()) {
					actor = authName;
				}
			}
		} catch (Exception ignored) {}

		orderTransLog.setCreatedBy(actor);
		orderTransLog.setCreatedAt(java.time.LocalDateTime.now());
		ordTransLogRepository.save(orderTransLog);

		log.info("[STATE-TRANSITION] {} >>> {} by {}", currentStatus, toState, actor);
	}

}
