package com.kbrooks.orderplatform.service;

import com.kbrooks.orderplatform.client.InventoryClient;
import com.kbrooks.orderplatform.client.NotificationClient;
import com.kbrooks.orderplatform.dto.*;
import com.kbrooks.orderplatform.exception.InsufficientStockException;
import com.kbrooks.orderplatform.exception.OrderNotFoundException;
import com.kbrooks.orderplatform.model.Order;
import com.kbrooks.orderplatform.model.OrderItem;
import com.kbrooks.orderplatform.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.notificationClient = notificationClient;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItemRequest> reserved = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            try {
                inventoryClient.reserve(itemRequest.getProductId(), itemRequest.getQuantity());
                reserved.add(itemRequest);
            } catch (RestClientException e) {
                rollbackReservations(reserved);
                throw new InsufficientStockException(itemRequest.getProductId(), e.getMessage());
            }
        }

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus("CONFIRMED");

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(itemRequest.getPrice());
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);

        try {
            notificationClient.sendOrderConfirmation(saved.getCustomerId(), saved.getId());
        } catch (Exception e) {
            log.warn("Failed to send order confirmation notification for order {}: {}", saved.getId(), e.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        return toResponse(orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    public OrderResponse updateOrderStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(request.getStatus());
        return toResponse(orderRepository.save(order));
    }

    public void deleteOrder(UUID id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }

    private void rollbackReservations(List<OrderItemRequest> reserved) {
        for (OrderItemRequest item : reserved) {
            try {
                inventoryClient.release(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to release inventory for product {} during rollback: {}",
                    item.getProductId(), e.getMessage());
            }
        }
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(order.getItems().stream().map(item -> {
            OrderItemResponse itemResponse = new OrderItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setProductId(item.getProductId());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setPrice(item.getPrice());
            return itemResponse;
        }).toList());
        return response;
    }
}
