package com.order_service.repository;

import com.order_service.entity.Product;
import com.order_service.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(ProductCategory category);
    void deleteByCategory(ProductCategory category);
}