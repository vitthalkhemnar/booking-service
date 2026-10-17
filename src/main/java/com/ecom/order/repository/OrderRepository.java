package com.ecom.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);

	List<Order> findByUserId(Long userId);
}
