package com.order_module_o.order_module.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> orderItems;
}