package com.ecom.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUsername(String username);

}
