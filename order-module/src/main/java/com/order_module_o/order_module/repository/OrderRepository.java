package com.order_module_o.order_module.repository;

import com.order_module_o.order_module.entity.Order;
import com.order_module_o.order_module.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    List<Order> findByStatus(OrderStatus status);
}
