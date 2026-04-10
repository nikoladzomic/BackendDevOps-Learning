package com.example.learning.service.impl;

import com.example.learning.dto.EmailMessage;
import com.example.learning.dto.OrderDTO;
import com.example.learning.dto.PagedResponse;
import com.example.learning.entity.*;
import com.example.learning.exception.ConflictException;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.messaging.EmailProducer;
import com.example.learning.repository.*;
import com.example.learning.service.CartService;
import com.example.learning.service.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock EmailProducer emailProducer;
    @Mock CartService cartService;

    @InjectMocks OrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setEnabled(true);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Keychron K2");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct.setActive(true);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>(List.of(testCartItem)));

        testCartItem.setCart(testCart);
    }

    // ─── createOrder ─────────────────────────────────────────────────────────

    @Test
    void createOrder_withValidCart_shouldCreateOrderAndReduceStock() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUser(testUser);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTotalPrice(new BigDecimal("199.98"));
        savedOrder.setItems(new ArrayList<>());

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any())).thenReturn(savedOrder);

        OrderDTO result = orderService.createOrder();

        assertThat(result).isNotNull();
        // Stock je smanjen za 2
        assertThat(testProduct.getStock()).isEqualTo(8);
        verify(cartService).clearCart();
    }

    @Test
    void createOrder_withEmptyCart_shouldThrowConflictException() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        testCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));

        assertThatThrownBy(() -> orderService.createOrder())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void createOrder_whenCartNotFound_shouldThrowConflictException() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder())
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOrder_withInsufficientStock_shouldThrowConflictException() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        testProduct.setStock(1); // ima 1, traži 2

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> orderService.createOrder())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough stock");

        // Stock se nije smanjio
        assertThat(testProduct.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_withInactiveProduct_shouldThrowConflictException() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        testProduct.setActive(false);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> orderService.createOrder())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void createOrder_whenEmailFails_shouldStillCreateOrder() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUser(testUser);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTotalPrice(new BigDecimal("199.98"));
        savedOrder.setItems(new ArrayList<>());

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(emailProducer).sendEmailMessage(any());

        // Ne sme da baci exception — email fail je non-fatal
        assertThatCode(() -> orderService.createOrder())
                .doesNotThrowAnyException();

        verify(cartService).clearCart();
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_validTransition_shouldUpdateStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithItemsAndUser(1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderDTO result = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateStatus_invalidTransition_shouldThrowConflictException() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.DELIVERED); // završena porudžbina
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithItemsAndUser(1L))
                .thenReturn(Optional.of(order));

        // Ne možeš da menjaš DELIVERED order
        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_cancelledToPending_shouldThrowConflictException() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.CANCELLED);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithItemsAndUser(1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_toShipped_shouldSendEmail() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithItemsAndUser(1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateStatus(1L, OrderStatus.SHIPPED);

        verify(emailProducer).sendEmailMessage(
                argThat(msg -> "ORDER_SHIPPED".equals(msg.getType())
                        && "test@test.com".equals(msg.getTo()))
        );
    }

    @Test
    void updateStatus_toConfirmed_shouldNotSendEmail() {
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithItemsAndUser(1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        // Email se šalje samo za SHIPPED
        verify(emailProducer, never()).sendEmailMessage(any());
    }

    // ─── getMyOrders ──────────────────────────────────────────────────────────

    @Test
    void getMyOrders_shouldReturnUserOrders() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setItems(new ArrayList<>());

        when(orderRepository.findByUserIdWithItems(1L))
                .thenReturn(List.of(order));

        List<OrderDTO> result = orderService.getMyOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getMyOrder_withNonExistentOrder_shouldThrowResourceNotFoundException() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(orderRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}