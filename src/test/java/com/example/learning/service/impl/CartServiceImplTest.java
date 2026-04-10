package com.example.learning.service.impl;

import com.example.learning.dto.AddToCartRequest;
import com.example.learning.dto.CartDTO;
import com.example.learning.dto.UpdateCartItemRequest;
import com.example.learning.entity.*;
import com.example.learning.exception.ConflictException;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.repository.CartRepository;
import com.example.learning.repository.ProductRepository;
import com.example.learning.repository.UserRepository;
import com.example.learning.service.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks CartServiceImpl cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Keychron K2");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct.setActive(true);

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
    }

    // ─── getCart ─────────────────────────────────────────────────────────────

    @Test
    void getCart_whenCartExists_shouldReturnCart() {
        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));

        CartDTO result = cartService.getCart();

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCart_whenCartNotExists_shouldCreateAndReturnNewCart() {
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.save(any())).thenReturn(testCart);

        CartDTO result = cartService.getCart();

        assertThat(result).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    // ─── addItem ─────────────────────────────────────────────────────────────

    @Test
    void addItem_withValidProduct_shouldAddToCart() {
        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));
        when(cartRepository.save(any())).thenReturn(testCart);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        cartService.addItem(request);

        assertThat(testCart.getItems()).hasSize(1);
        assertThat(testCart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_withInactiveProduct_shouldThrowConflictException() {
        testProduct.setActive(false);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void addItem_withInsufficientStock_shouldThrowConflictException() {
        testProduct.setStock(1);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(5); // traži 5, ima samo 1

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    void addItem_withExistingProductInCart_shouldMergeQuantity() {
        CartItem existingItem = new CartItem();
        existingItem.setId(1L);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(2);
        existingItem.setCart(testCart);
        testCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));
        when(cartRepository.save(any())).thenReturn(testCart);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        cartService.addItem(request);

        // Treba da ima 1 item sa quantity 5, ne 2 itema
        assertThat(testCart.getItems()).hasSize(1);
        assertThat(testCart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_mergeExceedsStock_shouldThrowConflictException() {
        testProduct.setStock(4);

        CartItem existingItem = new CartItem();
        existingItem.setId(1L);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(3); // već ima 3
        existingItem.setCart(testCart);
        testCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3); // 3+3=6 > stock 4

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(ConflictException.class);
    }

    // ─── updateItem ───────────────────────────────────────────────────────────

    @Test
    void updateItem_withValidQuantity_shouldUpdateQuantity() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setCart(testCart);
        testCart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(cartRepository.save(any())).thenReturn(testCart);

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        cartService.updateItem(1L, request);

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_exceedsStock_shouldThrowConflictException() {
        testProduct.setStock(3);

        CartItem item = new CartItem();
        item.setId(1L);
        item.setProduct(testProduct);
        item.setQuantity(1);
        item.setCart(testCart);
        testCart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(10);

        assertThatThrownBy(() -> cartService.updateItem(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    // ─── removeItem ───────────────────────────────────────────────────────────

    @Test
    void removeItem_withValidItemId_shouldRemoveFromCart() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setCart(testCart);
        testCart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));
        when(cartRepository.save(any())).thenReturn(testCart);

        cartService.removeItem(1L);

        assertThat(testCart.getItems()).isEmpty();
    }

    @Test
    void removeItem_withNonExistentItemId_shouldThrowResourceNotFoundException() {
        when(cartRepository.findByUserIdWithItems(1L))
                .thenReturn(Optional.of(testCart));

        assertThatThrownBy(() -> cartService.removeItem(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}