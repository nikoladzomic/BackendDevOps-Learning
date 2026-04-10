package com.example.learning.service.impl;

import com.example.learning.audit.Audited;
import com.example.learning.dto.*;
import com.example.learning.entity.*;
import com.example.learning.exception.*;
import com.example.learning.messaging.EmailProducer;
import com.example.learning.repository.*;
import com.example.learning.service.CartService;
import com.example.learning.service.CurrentUserProvider;
import com.example.learning.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final EmailProducer emailProducer;
    private final CartService cartService;

    @Override
    @Transactional
    @Audited(action = "CREATE_ORDER", resourceType = "ORDER")
    public OrderDTO createOrder() {
        Long userId = currentUserProvider.getCurrentUserId();

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ConflictException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ConflictException("Cannot create order from empty cart");
        }

        // Proveri stock i kreiraj order items
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProduct().getId()));

            if (!product.isActive()) {
                throw new ConflictException(
                        "Product is no longer available: " + product.getName());
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new ConflictException(
                        "Not enough stock for: " + product.getName()
                                + ". Available: " + product.getStock());
            }

            // Snapshot vrednosti u trenutku kupovine
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(subtotal);

            order.getItems().add(orderItem);
            totalPrice = totalPrice.add(subtotal);

            // Smanji stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        // Očisti korpu
        cartService.clearCart();

        // Pošalji email kroz RabbitMQ — sada se zaista koristi!
        String user = cart.getUser().getEmail();
        String link = "/orders/" + savedOrder.getId();

        try {
            emailProducer.sendEmailMessage(
                    new EmailMessage(user, "ORDER_CONFIRMATION", link)
            );
        } catch (Exception e) {
            // Email fail ne sme da pukne order
            log.warn("Failed to send order confirmation email for order {}: {}",
                    savedOrder.getId(), e.getMessage());
        }

        log.info("Order {} created for user {}", savedOrder.getId(), userId);
        return mapToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getMyOrders() {
        Long userId = currentUserProvider.getCurrentUserId();
        return orderRepository.findByUserIdWithItems(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getMyOrder(Long orderId) {
        Long userId = currentUserProvider.getCurrentUserId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
        return mapToDTO(order);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_ORDER_STATUS", resourceType = "ORDER")
    public OrderDTO updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItemsAndUser(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (newStatus == OrderStatus.SHIPPED) {
            try {
                emailProducer.sendEmailMessage(
                        new EmailMessage(
                                order.getUser().getEmail(),
                                "ORDER_SHIPPED",
                                "/orders/" + order.getId()
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to send shipped email for order {}: {}",
                        orderId, e.getMessage());
            }
        }

        return mapToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderDTO> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<OrderDTO> content = orderPage.getContent()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new ConflictException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private OrderDTO mapToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setItems(itemDTOs);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}