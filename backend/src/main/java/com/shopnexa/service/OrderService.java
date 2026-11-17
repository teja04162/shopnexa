package com.shopnexa.service;

import com.shopnexa.dto.CheckoutRequest;
import com.shopnexa.dto.CheckoutResponse;
import com.shopnexa.dto.OrderResponse;
import com.shopnexa.dto.PaymentOptionsResponse;
import com.shopnexa.dto.PaymentVerificationRequest;

public interface OrderService {

    PaymentOptionsResponse getPaymentOptions();

    CheckoutResponse checkout(String email, CheckoutRequest request);

    OrderResponse getOrder(String email, Long orderId);

    OrderResponse verifyPayment(String email, Long orderId, PaymentVerificationRequest request);

    OrderResponse cancelPendingPayment(String email, Long orderId);

    void expirePendingPayment(Long orderId);
}
