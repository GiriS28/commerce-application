package com.nisum.cartservice.repository;

import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.entity.CartItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RedisTemplate<String, Cart> redisTemplate;

    private final Long userId = 999L;

    @AfterEach
    void cleanup() {
        cartRepository.deleteByUserId(userId);
    }

    @Test
    void shouldSaveAndRetrieveCart() {

        CartItem item = new CartItem(
                1L,
                "Test Laptop",
                2,
                new BigDecimal("75000.00")
        );

        Cart cart = new Cart();
        cart.setCartId("CART-TEST-999");
        cart.setUserId(userId);
        cart.setItems(List.of(item));

        cartRepository.save(cart);

        Cart savedCart = cartRepository.findByUserId(userId);

        assertNotNull(savedCart);
        assertEquals("CART-TEST-999", savedCart.getCartId());
        assertEquals(userId, savedCart.getUserId());
        assertEquals(1, savedCart.getItems().size());

        CartItem savedItem = savedCart.getItems().get(0);

        assertEquals(1L, savedItem.getProductId());
        assertEquals("Test Laptop", savedItem.getProductName());
        assertEquals(2, savedItem.getQuantity());
        assertEquals(
                new BigDecimal("75000.00"),
                savedItem.getPriceSnapshot()
        );
    }

    @Test
    void shouldDeleteCart() {

        Cart cart = new Cart();
        cart.setCartId("CART-TEST-DELETE");
        cart.setUserId(userId);
        cart.setItems(List.of());

        cartRepository.save(cart);

        assertNotNull(cartRepository.findByUserId(userId));

        cartRepository.deleteByUserId(userId);

        assertNull(cartRepository.findByUserId(userId));
    }
}