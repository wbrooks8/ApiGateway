package com.kbrooks.orderplatform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void reserve(String productId, int quantity) {
        restTemplate.put(
            inventoryServiceUrl + "/inventory/{productId}/reserve",
            Map.of("quantity", quantity),
            productId
        );
    }

    public void release(String productId, int quantity) {
        restTemplate.put(
            inventoryServiceUrl + "/inventory/{productId}/release",
            Map.of("quantity", quantity),
            productId
        );
    }
}
