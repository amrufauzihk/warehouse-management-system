package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.Variant;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.web.dto.InventoryPositionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public InventoryService(ItemRepository itemRepository, VariantRepository variantRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryPositionResponse> list(Integer maxStock) {
        if (maxStock != null && maxStock < 0) {
            throw new BadRequestException("maxStock cannot be negative");
        }
        int limit = maxStock == null ? Integer.MAX_VALUE : maxStock;
        List<InventoryPositionResponse> positions = new ArrayList<>();
        for (Item item : itemRepository.findSellableItemsAtOrBelow(limit)) {
            positions.add(new InventoryPositionResponse(
                    "ITEM",
                    item.getId(),
                    null,
                    item.getSku(),
                    item.getName(),
                    item.getName(),
                    item.getPrice(),
                    item.getStockQuantity()
            ));
        }
        for (Variant variant : variantRepository.findSellableVariantsAtOrBelow(limit)) {
            positions.add(new InventoryPositionResponse(
                    "VARIANT",
                    variant.getItem().getId(),
                    variant.getId(),
                    variant.getSku(),
                    variant.getName(),
                    variant.getItem().getName(),
                    variant.getPrice(),
                    variant.getStockQuantity()
            ));
        }
        positions.sort(Comparator.comparing(InventoryPositionResponse::sku));
        return positions;
    }
}
