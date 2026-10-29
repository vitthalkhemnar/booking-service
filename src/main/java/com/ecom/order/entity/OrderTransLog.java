package com.ecom.order.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.ecom.order.util.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "order_translog")
public class OrderTransLog {
	
	@Id
	@Column(name = "translog_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long transLogId;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "from_state")
	private OrderStatus fromState;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "to_state")
	private OrderStatus toState;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;
	
	@Column(name = "created_by")
	private String createdBy;
	
	@CreatedDate
	@Column(name = "created_at")
	private LocalDateTime createdAt;

}
