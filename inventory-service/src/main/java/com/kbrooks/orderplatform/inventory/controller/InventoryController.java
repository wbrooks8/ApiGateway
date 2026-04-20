package com.kbrooks.orderplatform.inventory.controller;

import com.kbrooks.orderplatform.inventory.dto.InventoryRequest;
import com.kbrooks.orderplatform.inventory.dto.InventoryResponse;
import com.kbrooks.orderplatform.inventory.dto.ReserveRequest;
import com.kbrooks.orderplatform.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(@Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addInventory(request));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> updateQuantity(
            @PathVariable String productId,
            @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateQuantity(productId, request));
    }

    @PutMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserve(
            @PathVariable String productId,
            @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(productId, request));
    }

    @PutMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> release(
            @PathVariable String productId,
            @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.release(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteInventory(@PathVariable String productId) {
        inventoryService.deleteInventory(productId);
        return ResponseEntity.noContent().build();
    }
}
