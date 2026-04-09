package com.example.learning.repository;

import com.example.learning.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
    List<Product> findAllByCategoryIdAndActiveTrue(Long categoryId);
    boolean existsByNameAndCategoryId(String name, Long categoryId);
}