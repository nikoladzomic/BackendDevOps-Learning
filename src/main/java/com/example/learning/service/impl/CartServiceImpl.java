package com.example.learning.service.impl;

import com.example.learning.dto.*;
import com.example.learning.entity.*;
import com.example.learning.exception.*;
import com.example.learning.repository.*;
import com.example.learning.service.CartService;
import com.example.learning.service.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public CartDTO getCart() {
        Cart cart = getOrCreateCart();
        return mapToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addItem(AddToCartRequest request) {
        Cart cart = getOrCreateCart();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new ConflictException("Product is not available");
        }

        if (product.getStock() < request.getQuantity()) {
            throw new ConflictException("Not enough stock. Available: " + product.getStock());
        }

        // Ako već postoji u korpi — povećaj količinu
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new ConflictException("Not enough stock. Available: " + product.getStock());
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        return mapToDTO(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDTO updateItem(Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart();

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (item.getProduct().getStock() < request.getQuantity()) {
            throw new ConflictException(
                    "Not enough stock. Available: " + item.getProduct().getStock());
        }

        item.setQuantity(request.getQuantity());
        return mapToDTO(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDTO removeItem(Long cartItemId) {
        Cart cart = getOrCreateCart();

        boolean removed = cart.getItems()
                .removeIf(item -> item.getId().equals(cartItemId));

        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        return mapToDTO(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // Kreira korpu ako ne postoji — lazy initialization
    private Cart getOrCreateCart() {
        Long userId = currentUserProvider.getCurrentUserId();

        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartDTO mapToDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());

        BigDecimal totalPrice = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setItems(itemDTOs);
        dto.setTotalPrice(totalPrice);
        dto.setTotalItems(totalItems);
        dto.setUpdatedAt(cart.getUpdatedAt());
        return dto;
    }

    private CartItemDTO mapItemToDTO(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setUnitPrice(item.getProduct().getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(subtotal);
        return dto;
    }
}