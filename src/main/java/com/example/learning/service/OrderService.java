package com.example.learning.service;

import com.example.learning.dto.*;
import com.example.learning.entity.OrderStatus;
import java.util.List;

public interface OrderService {
    OrderDTO createOrder();
    List<OrderDTO> getMyOrders();
    OrderDTO getMyOrder(Long orderId);
    OrderDTO updateStatus(Long orderId, OrderStatus status);
    PagedResponse<OrderDTO> getAllOrders(int page, int size);
}