package com.nisum.cartservice.service;

import com.nisum.cartservice.client.ProductServiceClient;
import com.nisum.cartservice.dto.request.AddCartItemRequest;
import com.nisum.cartservice.dto.response.ProductResponse;
import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.entity.CartItem;
import com.nisum.cartservice.exception.CartItemNotFoundException;
import com.nisum.cartservice.exception.CartNotFoundException;
import com.nisum.cartservice.exception.ProductNotAvailableException;
import com.nisum.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class CartService {

    private final ProductServiceClient productServiceClient;

    private final CartRepository cartRepository;

    public CartService(ProductServiceClient productServiceClient, CartRepository cartRepository) {
        this.productServiceClient = productServiceClient;
        this.cartRepository = cartRepository;
    }

    public Cart getCart(Long userId) {


        Cart cart = cartRepository.findByUserId(userId);

        if (cart != null) {
            return cart;
        }

        return createCart(userId);
    }

    private Cart createCart(Long userId) {

        LocalDateTime now = LocalDateTime.now();

        Cart cart = new Cart();

        cart.setCartId("CART-" + UUID.randomUUID());
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        cart.setExpiresAt(now.plusMinutes(30));

        cartRepository.save(cart);

        return cart;
    }

    public Cart addItem(Long userId, AddCartItemRequest request) {

        ProductResponse product =
                productServiceClient.getProduct(request.productId());

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new ProductNotAvailableException(request.productId());
        }

        Cart cart = getCart(userId);

        CartItem existingItem = cart.getItems()
                .stream()
                .filter(item ->
                        item.getProductId().equals(product.id()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {

            existingItem.setQuantity(
                    existingItem.getQuantity() + request.quantity()
            );

        } else {

            CartItem newItem = new CartItem(
                    product.id(),
                    product.name(),
                    request.quantity(),
                    product.price()
            );

            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cart.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        refreshCartExpiration(cart);
        cartRepository.save(cart);

        return cart;
    }

    public Cart updateItemQuantity(
            Long userId,
            Long productId,
            Integer quantity) {

        Cart cart = cartRepository.findByUserId(userId);

        if (cart == null) {
            throw new CartNotFoundException(userId);
        }

        CartItem item = cart.getItems()
                .stream()
                .filter(cartItem ->
                        cartItem.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) {
            throw new CartItemNotFoundException(productId);
        }

        item.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        cart.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        refreshCartExpiration(cart);
        cartRepository.save(cart);

        return cart;
    }

    public Cart removeItem(Long userId, Long productId) {

        Cart cart = cartRepository.findByUserId(userId);

        if (cart == null) {
            throw new CartNotFoundException(userId);
        }

        boolean removed = cart.getItems()
                .removeIf(item ->
                        item.getProductId().equals(productId));

        if (!removed) {
            throw new CartItemNotFoundException(productId);
        }

        cart.setUpdatedAt(LocalDateTime.now());

        refreshCartExpiration(cart);
        cartRepository.save(cart);

        return cart;
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);

        if (cart == null) {
            throw new CartNotFoundException(userId);
        }

        cartRepository.deleteByUserId(userId);
    }

    private void refreshCartExpiration(Cart cart) {
        LocalDateTime now = LocalDateTime.now();

        cart.setUpdatedAt(now);
        cart.setExpiresAt(now.plusMinutes(30));
    }
}