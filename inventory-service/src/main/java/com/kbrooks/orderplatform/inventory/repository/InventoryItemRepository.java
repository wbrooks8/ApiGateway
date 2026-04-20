package com.kbrooks.orderplatform.inventory.repository;

import com.kbrooks.orderplatform.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByProductId(String productId);
    boolean existsByProductId(String productId);
}
