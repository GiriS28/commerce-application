package com.nisum.cartservice.repository;

import com.nisum.cartservice.config.ResilienceConfig;
import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.exception.CartStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class CartRepository {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_TTL_MINUTES = 30;

    private static final Logger log =
            LoggerFactory.getLogger(CartRepository.class);

    private final RedisTemplate<String, Cart> redisTemplate;

    public CartRepository(RedisTemplate<String, Cart> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Cart cart) {
        try {
            String key = buildKey(cart.getUserId());

            redisTemplate.opsForValue().set(
                    key,
                    cart,
                    CART_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            log.info(
                    "Redis SAVE: key={}, TTL={} seconds",
                    key,
                    ttl
            );
        } catch (Exception e) {
            throw new CartStorageException("Unable to access cart storage", e);
        }
    }

    public Cart findByUserId(Long userId) {
        try {
            String key = buildKey(userId);

            Cart cart = redisTemplate.opsForValue().get(key);

            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            log.info(
                    "Redis GET: key={}, found={}, TTL={} seconds",
                    key,
                    cart != null,
                    ttl
            );

            return cart;

        } catch (Exception exception) {
            throw new CartStorageException(
                    "Unable to access cart storage",
                    exception
            );
        }
    }

    public void deleteByUserId(Long userId) {
        try {
            String key = buildKey(userId);

            Boolean deleted = redisTemplate.delete(key);

            log.info(
                    "Redis DELETE: key={}, deleted={}",
                    key,
                    deleted
            );

        } catch (Exception e) {
            throw new CartStorageException("Unable to access cart storage", e);
        }
    }

    private String buildKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }


}