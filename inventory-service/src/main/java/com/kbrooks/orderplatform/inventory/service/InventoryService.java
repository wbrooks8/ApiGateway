package com.kbrooks.orderplatform.inventory.service;

import com.kbrooks.orderplatform.inventory.dto.InventoryRequest;
import com.kbrooks.orderplatform.inventory.dto.InventoryResponse;
import com.kbrooks.orderplatform.inventory.dto.ReserveRequest;
import com.kbrooks.orderplatform.inventory.exception.InventoryNotFoundException;
import com.kbrooks.orderplatform.inventory.model.InventoryItem;
import com.kbrooks.orderplatform.inventory.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository repository;

    public InventoryService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    public InventoryResponse addInventory(InventoryRequest request) {
        if (repository.existsByProductId(request.getProductId())) {
            throw new IllegalArgumentException("Inventory already exists for product: " + request.getProductId());
        }
        InventoryItem item = new InventoryItem();
        item.setProductId(request.getProductId());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setReservedQuantity(0);
        return toResponse(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(String productId) {
        return toResponse(repository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException(productId)));
    }

    public InventoryResponse updateQuantity(String productId, InventoryRequest request) {
        InventoryItem item = repository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        return toResponse(repository.save(item));
    }

    public InventoryResponse reserve(String productId, ReserveRequest request) {
        InventoryItem item = repository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        int available = item.getAvailableQuantity();
        if (request.getQuantity() > available) {
            throw new IllegalArgumentException(
                "Insufficient stock. Requested: " + request.getQuantity() + ", available: " + available);
        }
        item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
        return toResponse(repository.save(item));
    }

    public InventoryResponse release(String productId, ReserveRequest request) {
        InventoryItem item = repository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        int newReserved = item.getReservedQuantity() - request.getQuantity();
        if (newReserved < 0) {
            throw new IllegalArgumentException("Cannot release more than currently reserved quantity");
        }
        item.setReservedQuantity(newReserved);
        return toResponse(repository.save(item));
    }

    public void deleteInventory(String productId) {
        InventoryItem item = repository.findByProductId(productId)
            .orElseThrow(() -> new InventoryNotFoundException(productId));
        repository.delete(item);
    }

    private InventoryResponse toResponse(InventoryItem item) {
        InventoryResponse response = new InventoryResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setReservedQuantity(item.getReservedQuantity());
        response.setAvailableQuantity(item.getAvailableQuantity());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }
}
