package com.example.learning.service;

import com.example.learning.dto.*;

public interface CartService {
    CartDTO getCart();
    CartDTO addItem(AddToCartRequest request);
    CartDTO updateItem(Long cartItemId, UpdateCartItemRequest request);
    CartDTO removeItem(Long cartItemId);
    void clearCart();
}