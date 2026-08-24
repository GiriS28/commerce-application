package com.nisum.userservice.repository;

import com.nisum.userservice.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    Page<UserEntity> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String search, String search1, Pageable pageable);
}
