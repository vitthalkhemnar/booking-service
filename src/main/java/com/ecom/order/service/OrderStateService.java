package com.ecom.order.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.dto.OrderStateChangeRequest;
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
		
		order.setStatus(toState);
		orderRepository.save(order);
		
		OrderTransLog orderTransLog = new OrderTransLog();
		orderTransLog.setOrder(order);
		orderTransLog.setFromState(currentStatus);
		orderTransLog.setToState(toState);
		orderTransLog.setCreatedBy("SYSTEM");
		ordTransLogRepository.save(orderTransLog);

		log.info("[STATE-TRANSITION] {} >>> {}", currentStatus, toState);
	}

}
