package com.kbrooks.orderplatform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${services.notification.url}")
    private String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendOrderConfirmation(String customerId, UUID orderId) {
        Map<String, String> body = Map.of(
            "type", "ORDER_CONFIRMED",
            "recipient", customerId,
            "subject", "Order Confirmed - " + orderId,
            "message", "Your order " + orderId + " has been confirmed and inventory has been reserved."
        );
        restTemplate.postForEntity(notificationServiceUrl + "/notifications", body, Void.class);
    }

    public void sendOrderCancellation(String customerId, UUID orderId) {
        Map<String, String> body = Map.of(
            "type", "ORDER_CANCELLED",
            "recipient", customerId,
            "subject", "Order Cancelled - " + orderId,
            "message", "Your order " + orderId + " could not be processed due to insufficient stock."
        );
        restTemplate.postForEntity(notificationServiceUrl + "/notifications", body, Void.class);
    }
}
