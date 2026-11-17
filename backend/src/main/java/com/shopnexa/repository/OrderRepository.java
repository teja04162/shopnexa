package com.shopnexa.repository;

import com.shopnexa.entity.Order;
import com.shopnexa.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items.product", "payment"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items.product", "payment"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    List<Order> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus status, Instant createdAt);
}
