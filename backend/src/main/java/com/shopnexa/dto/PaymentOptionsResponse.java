package com.shopnexa.dto;

public record PaymentOptionsResponse(
        boolean cashOnDeliveryEnabled,
        boolean razorpayEnabled,
        String razorpayKeyId
) {
}
