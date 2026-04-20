package com.kbrooks.orderplatform.notification.service;

import com.kbrooks.orderplatform.notification.dto.NotificationRequest;
import com.kbrooks.orderplatform.notification.dto.NotificationResponse;
import com.kbrooks.orderplatform.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<Notification> store = new CopyOnWriteArrayList<>();

    public NotificationResponse send(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setType(request.getType());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());

        store.add(notification);
        log.info("Notification sent [{}] to {}: {}", notification.getType(), notification.getRecipient(), notification.getSubject());

        return toResponse(notification);
    }

    public List<NotificationResponse> getAll() {
        return store.stream().map(this::toResponse).toList();
    }

    public NotificationResponse getById(UUID id) {
        return store.stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .map(this::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse response = new NotificationResponse();
        response.setId(n.getId());
        response.setType(n.getType());
        response.setRecipient(n.getRecipient());
        response.setSubject(n.getSubject());
        response.setMessage(n.getMessage());
        response.setCreatedAt(n.getCreatedAt());
        return response;
    }
}
