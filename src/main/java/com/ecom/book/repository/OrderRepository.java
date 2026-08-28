package com.ecom.book.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.book.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUsername(String username);

}
