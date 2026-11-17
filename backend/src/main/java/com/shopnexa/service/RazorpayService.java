package com.shopnexa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.PaymentGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RazorpayService {

    private static final URI ORDERS_ENDPOINT = URI.create("https://api.razorpay.com/v1/orders");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");

    private final ObjectMapper objectMapper;
    private final String keyId;
    private final String keySecret;
    private final HttpClient httpClient;

    public RazorpayService(
            ObjectMapper objectMapper,
            @Value("${app.payment.razorpay-key-id:}") String keyId,
            @Value("${app.payment.razorpay-key-secret:}") String keySecret
    ) {
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public boolean isEnabled() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    public String getKeyId() {
        return isEnabled() ? keyId : null;
    }

    public GatewayOrder createOrder(BigDecimal total, String receipt) {
        if (!isEnabled()) {
            throw new BadRequestException("Online payment is not configured. Choose cash on delivery.");
        }

        long amountInPaise;
        try {
            amountInPaise = total.setScale(2).movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentGatewayException("The order total is outside the supported payment range.", exception);
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "amount", amountInPaise,
                    "currency", "INR",
                    "receipt", receipt
            ));
            String credentials = Base64.getEncoder()
                    .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(ORDERS_ENDPOINT)
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentGatewayException("Razorpay could not create a payment order.");
            }

            JsonNode result = objectMapper.readTree(response.body());
            String orderId = result.path("id").asText("");
            long returnedAmount = result.path("amount").asLong(-1);
            String currency = result.path("currency").asText("");
            if (orderId.isBlank() || returnedAmount != amountInPaise || !"INR".equals(currency)) {
                throw new PaymentGatewayException("Razorpay returned an invalid payment order.");
            }
            return new GatewayOrder(orderId, amountInPaise, currency);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Payment provider request was interrupted.", exception);
        } catch (IOException exception) {
            throw new PaymentGatewayException("Razorpay is unavailable. Please try again or choose cash on delivery.", exception);
        }
    }

    public void verifySignature(String orderId, String paymentId, String signature) {
        if (!isEnabled()) {
            throw new BadRequestException("Online payment verification is not configured.");
        }
        if (signature == null || !SIGNATURE_PATTERN.matcher(signature).matches()) {
            throw new BadRequestException("Payment signature is invalid.");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(
                    mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8))
            ).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BadRequestException("Payment signature is invalid.");
            }
        } catch (GeneralSecurityException exception) {
            throw new PaymentGatewayException("Payment signature could not be verified.", exception);
        }
    }

    public record GatewayOrder(String id, long amountInPaise, String currency) {
    }
}
