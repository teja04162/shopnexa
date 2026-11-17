package com.shopnexa.service.impl;

import com.shopnexa.entity.Order;
import com.shopnexa.enums.OrderStatus;
import com.shopnexa.repository.OrderRepository;
import com.shopnexa.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class PendingPaymentExpirationJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public PendingPaymentExpirationJob(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expirePendingPayments() {
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        for (Order order : orderRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                OrderStatus.PENDING_PAYMENT,
                cutoff
        )) {
            orderService.expirePendingPayment(order.getId());
        }
    }
}
