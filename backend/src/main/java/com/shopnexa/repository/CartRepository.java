package com.shopnexa.repository;

import com.shopnexa.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items.product", "items.product.category"})
    Optional<Cart> findByUserEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items.product", "items.product.category"})
    @Query("select c from Cart c where lower(c.user.email) = lower(:email)")
    Optional<Cart> findByUserEmailIgnoreCaseForUpdate(@Param("email") String email);
}
