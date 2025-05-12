package com.order_module_o.order_module.service;


import com.order_module_o.order_module.repository.OrderRepository;
import com.order_module_o.order_module.dto.OrderItemRequest;
import com.order_module_o.order_module.dto.OrderItemResponse;
import com.order_module_o.order_module.dto.OrderRequest;
import com.order_module_o.order_module.dto.OrderResponse;
import com.order_module_o.order_module.entity.Order;
import com.order_module_o.order_module.entity.OrderItem;
import com.order_module_o.order_module.entity.OrderStatus;
import com.order_service.entity.Product;
import com.order_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // Validate products and calculate total
        List<OrderItem> orderItems = orderRequest.getOrderItems().stream()
                .map(this::createOrderItem)
                .collect(Collectors.toList());

        double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        // Create order
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setLastUpdated(LocalDateTime.now());

        // Set order reference in order items
        Order finalOrder = order;
        orderItems.forEach(item -> item.setOrder(finalOrder));

        // Save order
        order = orderRepository.save(order);

        // Map to response
        return mapToOrderResponse(order);
    }

    private OrderItem createOrderItem(OrderItemRequest itemRequest) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemRequest.getProductId()));

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(itemRequest.getQuantity());
        orderItem.setUnitPrice(product.getPrice());
        return orderItem;
    }

    public List<OrderResponse> getOrdersByUser(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        order.setStatus(newStatus);
        order.setLastUpdated(LocalDateTime.now());
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be canceled");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setLastUpdated(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        orderRepository.delete(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setOrderItems(order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList()));
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setOrderDate(order.getOrderDate());
        response.setLastUpdated(order.getLastUpdated());
        return response;
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        return response;
    }
}
