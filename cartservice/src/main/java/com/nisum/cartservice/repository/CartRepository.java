package com.nisum.cartservice.repository;

import com.nisum.cartservice.entity.Cart;
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

    public boolean save(Cart cart) {

        String key = buildKey(cart.getUserId());

        try {

            redisTemplate.opsForValue().set(
                    key,
                    cart,
                    CART_TTL_MINUTES,
                    TimeUnit.MINUTES
            );

            Long ttl =
                    redisTemplate.getExpire(key, TimeUnit.SECONDS);

            log.info(
                    "Redis SAVE successful: key={}, userId={}, TTL={} seconds",
                    key,
                    cart.getUserId(),
                    ttl
            );
            return true;

        } catch (Exception exception) {

            log.error(
                    "Redis SAVE failed: key={}, userId={}. " +
                            "Cart remains persisted in MySQL",
                    key,
                    cart.getUserId(),
                    exception
            );

            // Redis is only a cache.
            // Do not fail the request when cache update fails.
        }
        return false;
    }

    public Cart findByUserId(Long userId) {

        String key = buildKey(userId);

        try {

            Cart cart =
                    redisTemplate.opsForValue().get(key);

            Long ttl =
                    redisTemplate.getExpire(key, TimeUnit.SECONDS);

            if (cart != null) {

                log.info(
                        "Redis cache HIT: key={}, userId={}, TTL={} seconds",
                        key,
                        userId,
                        ttl
                );

            } else {

                log.info(
                        "Redis cache MISS: key={}, userId={}",
                        key,
                        userId
                );
            }

            return cart;

        } catch (Exception exception) {

            log.error(
                    "Redis GET failed: key={}, userId={}. " +
                            "Falling back to MySQL",
                    key,
                    userId,
                    exception
            );

            /*
             * Returning null intentionally.
             *
             * CartService interprets null as a Redis MISS
             * and then checks MySQL.
             */
            return null;
        }
    }

    public boolean deleteByUserId(Long userId) {

        String key = buildKey(userId);

        try {

            Boolean deleted =
                    redisTemplate.delete(key);

            log.info(
                    "Redis DELETE successful: key={}, userId={}, deleted={}",
                    key,
                    userId,
                    deleted
            );

            return Boolean.TRUE.equals(deleted);

        } catch (Exception exception) {

            log.error(
                    "Redis DELETE failed: key={}, userId={}. " +
                            "MySQL remains the source of truth",
                    key,
                    userId,
                    exception
            );

            return false;
        }
    }

    private String buildKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
}