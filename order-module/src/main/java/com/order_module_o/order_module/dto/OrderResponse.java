package com.order_module_o.order_module.dto;

import com.order_module_o.order_module.entity.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
    public class OrderResponse {
        private Long id;
        private String userId;
        private List<OrderItemResponse> orderItems;
        private double totalAmount;
        private OrderStatus status;
        private LocalDateTime orderDate;
        private LocalDateTime lastUpdated;
    }

