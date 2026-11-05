package com.ecom.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.order.entity.OrderTransLog;
import com.ecom.order.entity.Order;
import com.ecom.order.util.OrderStatus;

public interface OrderTransLogRepository extends JpaRepository<OrderTransLog, Long> {
	
	boolean existsByOrderAndFromStateAndToState(Order order, OrderStatus fromState, OrderStatus toState);

	List<OrderTransLog> findByOrderOrderByTransLogIdAsc(Order order);

	List<OrderTransLog> findByOrder_OrderIdOrderByTransLogIdAsc(Long orderId);

	@Modifying
	@Transactional
	void deleteByOrder(Order order);

	@Modifying
	@Transactional
	void deleteByOrder_OrderId(Long orderId);
}
