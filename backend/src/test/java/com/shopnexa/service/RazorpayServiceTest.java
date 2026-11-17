package com.shopnexa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopnexa.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RazorpayServiceTest {

    private static final String SECRET = "test-secret";

    private final RazorpayService razorpayService =
            new RazorpayService(new ObjectMapper(), "rzp_test_key", SECRET);

    @Test
    void verifiesValidOrderPaymentSignature() throws Exception {
        String signature = signature("order_test_123", "pay_test_456");

        razorpayService.verifySignature("order_test_123", "pay_test_456", signature);
    }

    @Test
    void rejectsModifiedOrderPaymentSignature() {
        assertThrows(
                BadRequestException.class,
                () -> razorpayService.verifySignature("order_test_123", "pay_test_456", "0".repeat(64))
        );
    }

    private String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(
                mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8))
        );
    }
}
