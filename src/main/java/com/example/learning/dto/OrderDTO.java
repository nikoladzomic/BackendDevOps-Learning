package com.example.learning.dto;

import com.example.learning.entity.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemDTO> items;
    private Instant createdAt;
    private Instant updatedAt;
}