package com.example.learning.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String imageUrl;
    private boolean active;
    private Long categoryId;
    private String categoryName;
    private Instant createdAt;
    private Instant updatedAt;
}