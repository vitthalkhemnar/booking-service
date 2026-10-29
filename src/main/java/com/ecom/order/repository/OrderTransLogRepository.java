package com.ecom.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.OrderTransLog;
import com.ecom.order.entity.Order;
import com.ecom.order.util.OrderStatus;


public interface OrderTransLogRepository extends JpaRepository<OrderTransLog, Long> {
	
	boolean existsByOrderAndFromStateAndToState(Order order, OrderStatus fromState, OrderStatus toState);
}
