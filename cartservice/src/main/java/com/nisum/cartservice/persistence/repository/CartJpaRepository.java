package com.nisum.cartservice.persistence.repository;

import com.nisum.cartservice.persistence.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
