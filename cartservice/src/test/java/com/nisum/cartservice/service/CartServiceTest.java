package com.nisum.cartservice.service;

import com.nisum.cartservice.client.ProductServiceClient;
import com.nisum.cartservice.dto.request.AddCartItemRequest;
import com.nisum.cartservice.dto.response.ProductResponse;
import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.entity.CartItem;
import com.nisum.cartservice.exception.ProductNotAvailableException;
import com.nisum.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private CartRepository cartRepository;
    private ProductServiceClient productServiceClient;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        productServiceClient = mock(ProductServiceClient.class);

        cartService = new CartService(
                productServiceClient,
                cartRepository
        );
    }

    @Test
    void shouldReturnExistingCart() {

        Cart cart = new Cart();
        cart.setCartId("CART-123");
        cart.setUserId(101L);

        when(cartRepository.findByUserId(101L))
                .thenReturn(cart);

        Cart result = cartService.getCart(101L);

        assertNotNull(result);
        assertEquals("CART-123", result.getCartId());
        assertEquals(101L, result.getUserId());

        verify(cartRepository).findByUserId(101L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldCreateCartWhenCartDoesNotExist() {

        when(cartRepository.findByUserId(101L))
                .thenReturn(null);

        Cart result = cartService.getCart(101L);

        assertNotNull(result);
        assertNotNull(result.getCartId());
        assertEquals(101L, result.getUserId());
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getExpiresAt());

        verify(cartRepository).findByUserId(101L);
        verify(cartRepository).save(result);
    }

    @Test
    void shouldAddNewProductToCart() {

        Cart cart = new Cart();
        cart.setCartId("CART-123");
        cart.setUserId(101L);
        cart.setItems(new ArrayList<>());
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        ProductResponse product = new ProductResponse(
                4L,
                "DELL-INSPIRON-15",
                "Dell Inspiron 15",
                "Dell 15 inch productivity laptop",
                new BigDecimal("59999.00"),
                "LAPTOP",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                2
        );

        when(productServiceClient.getProduct(4L))
                .thenReturn(product);

        when(cartRepository.findByUserId(101L))
                .thenReturn(cart);

        AddCartItemRequest request =
                new AddCartItemRequest(4L, 2);

        Cart result = cartService.addItem(101L, request);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        CartItem item = result.getItems().get(0);

        assertEquals(4L, item.getProductId());
        assertEquals("Dell Inspiron 15", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(
                new BigDecimal("59999.00"),
                item.getPriceSnapshot()
        );

        verify(productServiceClient).getProduct(4L);
        verify(cartRepository).findByUserId(101L);
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldIncreaseQuantityWhenProductAlreadyExists() {

        CartItem existingItem = new CartItem(
                4L,
                "Dell Inspiron 15",
                2,
                new BigDecimal("59999.00")
        );

        Cart cart = new Cart();
        cart.setCartId("CART-123");
        cart.setUserId(101L);
        cart.setItems(new ArrayList<>(List.of(existingItem)));
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        ProductResponse product = new ProductResponse(
                4L,
                "DELL-INSPIRON-15",
                "Dell Inspiron 15",
                "Dell 15 inch productivity laptop",
                new BigDecimal("59999.00"),
                "LAPTOP",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                2
        );

        when(productServiceClient.getProduct(4L))
                .thenReturn(product);

        when(cartRepository.findByUserId(101L))
                .thenReturn(cart);

        AddCartItemRequest request =
                new AddCartItemRequest(4L, 2);

        Cart result = cartService.addItem(101L, request);

        assertEquals(1, result.getItems().size());

        CartItem item = result.getItems().get(0);

        assertEquals(4L, item.getProductId());
        assertEquals(4, item.getQuantity());

        verify(productServiceClient).getProduct(4L);
        verify(cartRepository).findByUserId(101L);
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldRejectInactiveProduct() {

        ProductResponse product = new ProductResponse(
                4L,
                "DELL-INSPIRON-15",
                "Dell Inspiron 15",
                "Dell 15 inch productivity laptop",
                new BigDecimal("59999.00"),
                "LAPTOP",
                "INACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                2
        );

        when(productServiceClient.getProduct(4L))
                .thenReturn(product);

        AddCartItemRequest request =
                new AddCartItemRequest(4L, 1);

        assertThrows(
                ProductNotAvailableException.class,
                () -> cartService.addItem(101L, request)
        );

        verify(productServiceClient).getProduct(4L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingItemQuantity() {

        CartItem item = new CartItem(
                4L,
                "Dell Inspiron 15",
                5,
                new BigDecimal("59999.00")
        );

        Cart cart = new Cart();
        cart.setCartId("CART-123");
        cart.setUserId(101L);
        cart.setItems(new ArrayList<>(List.of(item)));
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        when(cartRepository.findByUserId(101L))
                .thenReturn(cart);

        Cart result =
                cartService.updateItemQuantity(101L, 4L, 3);

        assertEquals(1, result.getItems().size());
        assertEquals(3, result.getItems().get(0).getQuantity());

        verify(cartRepository).findByUserId(101L);
        verify(cartRepository).save(cart);
    }


}