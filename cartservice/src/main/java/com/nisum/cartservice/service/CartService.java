package com.nisum.cartservice.service;

import com.nisum.cartservice.client.ProductServiceClient;
import com.nisum.cartservice.dto.request.AddCartItemRequest;
import com.nisum.cartservice.dto.response.ProductResponse;
import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.exception.CartItemNotFoundException;
import com.nisum.cartservice.exception.CartNotFoundException;
import com.nisum.cartservice.exception.CartStorageException;
import com.nisum.cartservice.exception.ProductNotAvailableException;
import com.nisum.cartservice.mapper.CartMapper;
import com.nisum.cartservice.persistence.entity.CartEntity;
import com.nisum.cartservice.persistence.entity.CartItemEntity;
import com.nisum.cartservice.persistence.repository.CartJpaRepository;
import com.nisum.cartservice.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class CartService {

    private static final Logger log =
            LoggerFactory.getLogger(CartService.class);

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

        log.info(
                "Fetching cart: userId={}",
                userId
        );

        // 1. Check Redis first
        Cart cart = cartRepository.findByUserId(userId);

        if (cart != null) {

            log.info(
                    "Cart retrieved from Redis: userId={}, cartId={}",
                    userId,
                    cart.getCartId()
            );

            return cart;
        }

        log.info(
                "Cart not found in Redis. Checking MySQL: userId={}",
                userId
        );

        // 2. Redis MISS → check MySQL
        CartEntity cartEntity = findCartFromDatabase(userId);

        if (cartEntity == null) {
            throw new CartNotFoundException(userId);
        }

        if (cartEntity != null) {

            log.info(
                    "Cart found in MySQL: userId={}, cartId={}",
                    userId,
                    cartEntity.getId()
            );

            // 3. MySQL HIT → convert Entity to Domain
            Cart dbCart =
                    cartMapper.toDomain(cartEntity);

            // 4. Rebuild Redis cache
            boolean cacheUpdated =
                    cartRepository.save(dbCart);

            if (cacheUpdated) {

                log.info(
                        "Cart cache rebuilt in Redis: userId={}, cartId={}",
                        userId,
                        dbCart.getCartId()
                );

            } else {

                log.warn(
                        "Cart cache rebuild skipped: userId={}, cartId={}, " +
                                "reason=Redis unavailable",
                        userId,
                        dbCart.getCartId()
                );
            }

            return dbCart;
        }

        log.info(
                "Cart not found in Redis or MySQL. Creating new cart: userId={}",
                userId
        );

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

        // Persist in MySQL
        CartEntity cartEntity =
                cartMapper.toNewEntity(cart);

        CartEntity savedEntity =
                cartJpaRepository.save(cartEntity);

        log.info(
                "Cart created successfully in MySQL: userId={}, cartId={}",
                userId,
                savedEntity.getId()
        );

        // Convert persisted entity back to domain
        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        // Cache in Redis
        boolean cacheUpdated =
                cartRepository.save(savedCart);

        if (cacheUpdated) {

            log.info(
                    "Cart cache created in Redis: userId={}, cartId={}",
                    userId,
                    savedCart.getCartId()
            );

        } else {

            log.warn(
                    "Cart cache creation skipped: userId={}, cartId={}, " +
                            "reason=Redis unavailable",
                    userId,
                    savedCart.getCartId()
            );
        }

        return savedCart;
    }

    public Cart addItem(
            Long userId,
            AddCartItemRequest request) {

        log.info(
                "Adding item to cart: userId={}, productId={}, quantity={}",
                userId,
                request.productId(),
                request.quantity()
        );

        // 1. Validate product with Product Service
        ProductResponse product =
                productServiceClient.getProduct(request.productId());

        log.info(
                "Product validated: productId={}, status={}",
                product.id(),
                product.status()
        );

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {

            log.warn(
                    "Product is not active: userId={}, productId={}, status={}",
                    userId,
                    product.id(),
                    product.status()
            );

            throw new ProductNotAvailableException(
                    request.productId()
            );
        }

        // 2. Find the persistent cart from MySQL
        CartEntity cartEntity = findCartFromDatabase(userId);

        if (cartEntity == null) {
            throw new CartNotFoundException(userId);
        }

        log.debug(
                "Persistent cart found: userId={}, cartEntityId={}",
                userId,
                cartEntity.getId()
        );

        // 3. Check whether the product already exists
        CartItemEntity existingItem =
                cartEntity.getItems()
                        .stream()
                        .filter(item ->
                                item.getProductId().equals(product.id()))
                        .findFirst()
                        .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (existingItem != null) {

            // Product already exists → UPDATE existing row
            int newQuantity =
                    existingItem.getQuantity() + request.quantity();

            existingItem.setQuantity(newQuantity);
            existingItem.setUpdatedAt(now);

            log.info(
                    "Existing cart item quantity increased: " +
                            "userId={}, productId={}, oldQuantity={}, " +
                            "addedQuantity={}, newQuantity={}",
                    userId,
                    product.id(),
                    newQuantity - request.quantity(),
                    request.quantity(),
                    newQuantity
            );

        } else {

            // Product doesn't exist → INSERT new row
            CartItemEntity newItem = new CartItemEntity();

            newItem.setProductId(product.id());
            newItem.setProductName(product.name());
            newItem.setQuantity(request.quantity());
            newItem.setPriceSnapshot(product.price());
            newItem.setCreatedAt(now);
            newItem.setUpdatedAt(now);

            // VERY IMPORTANT:
            // addItem() sets both sides of the relationship.
            cartEntity.addItem(newItem);

            log.info(
                    "New cart item created: " +
                            "userId={}, productId={}, quantity={}",
                    userId,
                    product.id(),
                    request.quantity()
            );
        }

        // 4. Update cart timestamps
        cartEntity.setUpdatedAt(now);
        cartEntity.setExpiresAt(now.plusMinutes(30));

        // 5. Save MySQL
        CartEntity savedEntity =
                saveCartToDatabase(cartEntity);

        log.info(
                "Cart persisted successfully in MySQL: " +
                        "userId={}, cartEntityId={}",
                userId,
                savedEntity.getId()
        );

        // 6. Convert Entity → Domain
        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        // 7. Update Redis cache
        boolean cacheUpdated =
                cartRepository.save(savedCart);

        if (cacheUpdated) {

            log.info(
                    "Cart cache updated in Redis: userId={}, cartId={}",
                    userId,
                    savedCart.getCartId()
            );

        } else {

            log.warn(
                    "Cart cache update skipped: " +
                            "userId={}, cartId={}, " +
                            "reason=Redis unavailable. MySQL remains source of truth.",
                    userId,
                    savedCart.getCartId()
            );
        }

        log.info(
                "Cart item added successfully: " +
                        "userId={}, productId={}, quantity={}",
                userId,
                product.id(),
                request.quantity()
        );

        return savedCart;
    }

    public Cart updateItemQuantity(
            Long userId,
            Long productId,
            Integer quantity) {

        log.info(
                "Updating cart item quantity: userId={}, productId={}, quantity={}",
                userId,
                productId,
                quantity
        );

        CartEntity cartEntity = findCartFromDatabase(userId);

        if (cartEntity == null) {
            throw new CartNotFoundException(userId);
        }

        CartItemEntity item = cartEntity.getItems()
                .stream()
                .filter(cartItem ->
                        cartItem.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new CartItemNotFoundException(productId));

        item.setQuantity(quantity);

        LocalDateTime now = LocalDateTime.now();

        item.setUpdatedAt(now);
        cartEntity.setUpdatedAt(now);
        cartEntity.setExpiresAt(now.plusMinutes(30));

        CartEntity savedEntity =
                saveCartToDatabase(cartEntity);

        log.info(
                "Cart item quantity persisted in MySQL: " +
                        "userId={}, productId={}, quantity={}",
                userId,
                productId,
                quantity
        );

        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        boolean cacheUpdated =
                cartRepository.save(savedCart);

        if (cacheUpdated) {

            log.info(
                    "Cart cache updated in Redis after quantity change: " +
                            "userId={}, productId={}",
                    userId,
                    productId
            );

        } else {

            log.warn(
                    "Cart cache update skipped after quantity change: " +
                            "userId={}, productId={}, reason=Redis unavailable",
                    userId,
                    productId
            );
        }

        log.info(
                "Cart item quantity updated successfully: " +
                        "userId={}, productId={}, quantity={}",
                userId,
                productId,
                quantity
        );

        return savedCart;
    }

    public Cart removeItem(
            Long userId,
            Long productId) {

        log.info(
                "Removing cart item: userId={}, productId={}",
                userId,
                productId
        );

        CartEntity cartEntity = findCartFromDatabase(userId);

        if (cartEntity == null) {
            throw new CartNotFoundException(userId);
        }

        CartItemEntity item = cartEntity.getItems()
                .stream()
                .filter(cartItem ->
                        cartItem.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new CartItemNotFoundException(productId));

        cartEntity.removeItem(item);

        LocalDateTime now = LocalDateTime.now();

        cartEntity.setUpdatedAt(now);
        cartEntity.setExpiresAt(now.plusMinutes(30));

        CartEntity savedEntity =
                saveCartToDatabase(cartEntity);

        log.info(
                "Cart item removed from MySQL: userId={}, productId={}",
                userId,
                productId
        );

        Cart savedCart =
                cartMapper.toDomain(savedEntity);

        boolean cacheUpdated =
                cartRepository.save(savedCart);

        if (cacheUpdated) {

            log.info(
                    "Cart cache updated in Redis after item removal: " +
                            "userId={}, productId={}",
                    userId,
                    productId
            );

        } else {

            log.warn(
                    "Cart cache update skipped after item removal: " +
                            "userId={}, productId={}, reason=Redis unavailable",
                    userId,
                    productId
            );
        }

        log.info(
                "Cart item removed successfully: userId={}, productId={}",
                userId,
                productId
        );

        return savedCart;
    }

    public void clearCart(Long userId) {

        log.info(
                "Clearing cart: userId={}",
                userId
        );

        CartEntity cartEntity = findCartFromDatabase(userId);

        if (cartEntity == null) {
            throw new CartNotFoundException(userId);
        }

        Long cartEntityId = cartEntity.getId();

        deleteCartFromDatabase(cartEntity);

        log.info(
                "Cart deleted from MySQL: userId={}, cartEntityId={}",
                userId,
                cartEntityId
        );

        boolean cacheDeleted =
                cartRepository.deleteByUserId(userId);

        if (cacheDeleted) {

            log.info(
                    "Cart deleted from Redis: userId={}",
                    userId
            );

        } else {

            log.warn(
                    "Cart deletion from Redis skipped: " +
                            "userId={}, reason=Redis unavailable",
                    userId
            );
        }

        log.info(
                "Cart cleared successfully: userId={}",
                userId
        );
    }

    private void refreshCartExpiration(Cart cart) {

        LocalDateTime now = LocalDateTime.now();

        cart.setUpdatedAt(now);
        cart.setExpiresAt(now.plusMinutes(30));
    }

    private CartEntity findCartFromDatabase(Long userId) {

        try {

            return cartJpaRepository.findByUserId(userId)
                    .orElse(null);

        } catch (DataAccessException exception) {

            log.error(
                    "MySQL unavailable while fetching cart: userId={}",
                    userId,
                    exception
            );

            throw new CartStorageException(
                    "Unable to access cart database",
                    exception
            );
        }
    }

    private CartEntity saveCartToDatabase(CartEntity cartEntity) {

        try {

            return cartJpaRepository.save(cartEntity);

        } catch (DataAccessException exception) {

            log.error(
                    "MySQL unavailable while saving cart: cartEntityId={}, userId={}",
                    cartEntity.getId(),
                    cartEntity.getUserId(),
                    exception
            );

            throw new CartStorageException(
                    "Unable to save cart",
                    exception
            );
        }
    }

    private void deleteCartFromDatabase(CartEntity cartEntity) {

        try {

            cartJpaRepository.delete(cartEntity);

        } catch (DataAccessException exception) {

            log.error(
                    "MySQL unavailable while deleting cart: cartEntityId={}, userId={}",
                    cartEntity.getId(),
                    cartEntity.getUserId(),
                    exception
            );

            throw new CartStorageException(
                    "Unable to delete cart",
                    exception
            );
        }
    }
}