package com.example.learning.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductFilterRequest {
    private String name;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean active;
    private int page = 0;
    private int size = 12;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}