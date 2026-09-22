package com.nisum.cartservice.service;

import com.nisum.cartservice.client.ProductServiceClient;
import com.nisum.cartservice.dto.request.AddCartItemRequest;
import com.nisum.cartservice.dto.response.ProductResponse;
import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.entity.CartItem;
import com.nisum.cartservice.exception.CartItemNotFoundException;
import com.nisum.cartservice.exception.CartNotFoundException;
import com.nisum.cartservice.exception.ProductNotAvailableException;
import com.nisum.cartservice.mapper.CartMapper;
import com.nisum.cartservice.persistence.entity.CartEntity;
import com.nisum.cartservice.persistence.entity.CartItemEntity;
import com.nisum.cartservice.persistence.repository.CartJpaRepository;
import com.nisum.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class CartService {

    private final ProductServiceClient productServiceClient;

    private final CartRepository cartRepository;

    private final CartJpaRepository cartJpaRepository;

    private final CartMapper cartMapper;

    public CartService(
            ProductServiceClient productServiceClient,
            CartRepository cartRepository,
            CartJpaRepository cartJpaRepository,
            CartMapper cartMapper) {
        this.productServiceClient = productServiceClient;
        this.cartRepository = cartRepository;
        this.cartJpaRepository = cartJpaRepository;
        this.cartMapper = cartMapper;
    }

    public Cart getCart(Long userId) {

        // 1. Check Redis first
        Cart cart = cartRepository.findByUserId(userId);

        if (cart != null) {
            return cart;
        }

        // 2. Redis MISS → check MySQL
        CartEntity cartEntity =
                cartJpaRepository.findByUserId(userId)
                        .orElse(null);

        if (cartEntity != null) {

            // 3. MySQL HIT → convert Entity to Domain
            Cart dbCart =
                    cartMapper.toDomain(cartEntity);

            // 4. Rebuild Redis cache
            cartRepository.save(dbCart);

            return dbCart;
        }

        // 5. Cart doesn't exist anywhere → create it
        return createCart(userId);
    }

    private Cart createCart(Long userId) {

        LocalDateTime now = LocalDateTime.now();

        Cart cart = new Cart();

        cart.setCartId("CART-" + userId);
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        cart.setExpiresAt(now.plusMinutes(30));

        // 1. Persist in MySQL
        CartEntity cartEntity =
                cartMapper.toNewEntity(cart);

        CartEntity savedEntity =
                cartJpaRepository.save(cartEntity);

        // 2. Convert persisted entity back to domain
        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        // 3. Cache in Redis
        cartRepository.save(savedCart);

        return savedCart;
    }

    public Cart addItem(Long userId, AddCartItemRequest request) {

        // 1. Validate product with Product Service
        ProductResponse product =
                productServiceClient.getProduct(request.productId());

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new ProductNotAvailableException(request.productId());
        }

        // 2. Get cart
        Cart cart = getCart(userId);

        // 3. Check whether product already exists
        CartItem dbItem = cart.getItems()
                .stream()
                .filter(item ->
                        item.getProductId().equals(product.id()))
                .findFirst()
                .orElse(null);

        if (dbItem != null) {

            dbItem.setQuantity(
                    dbItem.getQuantity() + request.quantity()
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

        // 4. Refresh cart timestamps
        refreshCartExpiration(cart);

        // 5. Find existing persistent cart
        CartEntity cartEntity =
                cartJpaRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new CartNotFoundException(userId));

        // 6. Update existing CartEntity
        cartMapper.updateEntity(cart, cartEntity);

        // 7. Synchronize items
        // cartEntity.getItems().clear();

        for (CartItem item : cart.getItems()) {

            CartItemEntity existingItem =
                    cartEntity.getItems()
                            .stream()
                            .filter(entity ->
                                    entity.getProductId().equals(item.getProductId()))
                            .findFirst()
                            .orElse(null);

            if (existingItem != null) {

                existingItem.setProductName(item.getProductName());
                existingItem.setQuantity(item.getQuantity());
                existingItem.setPriceSnapshot(item.getPriceSnapshot());

            } else {

                CartItemEntity newItem =
                        cartMapper.toNewEntity(item);

                cartEntity.addItem(newItem);
            }
        }

        // 8. Save MySQL
        CartEntity savedEntity =
                cartJpaRepository.save(cartEntity);

        // 9. Convert back to domain
        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        // 10. Update Redis
        cartRepository.save(savedCart);

        return savedCart;
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