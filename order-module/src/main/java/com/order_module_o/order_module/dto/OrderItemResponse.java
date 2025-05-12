package com.order_module_o.order_module.dto;

import lombok.Data;

@Data
public class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private double unitPrice;
    }

