package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.MovementType;
import com.geli.warehouse.domain.Sale;
import com.geli.warehouse.domain.StockMovement;
import com.geli.warehouse.domain.Text;
import com.geli.warehouse.domain.Variant;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.BusinessRuleException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.StockMovementRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.web.dto.StockMovementResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final StockMovementRepository movementRepository;
    private final WarehouseMapper mapper;

    public StockService(
            ItemRepository itemRepository,
            VariantRepository variantRepository,
            StockMovementRepository movementRepository,
            WarehouseMapper mapper
    ) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.movementRepository = movementRepository;
        this.mapper = mapper;
    }

    @Transactional
    public StockMovementResponse adjustItem(Long itemId, int delta, String note) {
        return mapper.toMovement(applyToItem(itemId, delta, MovementType.ADJUSTMENT, note, null));
    }

    @Transactional
    public StockMovementResponse adjustVariant(Long variantId, int delta, String note) {
        return mapper.toMovement(applyToVariant(variantId, delta, MovementType.ADJUSTMENT, note, null));
    }

    @Transactional
    public StockMovement applyToItem(Long itemId, int delta, MovementType type, String note, Sale sale) {
        requireDelta(delta);
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NotFoundException("Item " + itemId + " was not found"));
        if (type != MovementType.SALE && variantRepository.existsByItem_IdAndActiveTrue(itemId)) {
            throw new BusinessRuleException("This item has variants. Adjust stock on a variant instead.");
        }
        if (type == MovementType.SALE) {
            assertItemSellable(item);
        }
        int balance = item.applyStockDelta(delta);
        return movementRepository.save(movement(item, null, type, delta, balance, note, sale));
    }

    @Transactional
    public StockMovement applyToVariant(Long variantId, int delta, MovementType type, String note, Sale sale) {
        requireDelta(delta);
        Variant variant = variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new NotFoundException("Variant " + variantId + " was not found"));
        if (type == MovementType.SALE) {
            assertVariantSellable(variant);
        }
        int balance = variant.applyStockDelta(delta);
        return movementRepository.save(movement(variant.getItem(), variant, type, delta, balance, note, sale));
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> itemMovements(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new NotFoundException("Item " + itemId + " was not found");
        }
        return movementRepository.findTop100ByItem_IdAndVariantIsNullOrderByIdDesc(itemId).stream()
                .map(mapper::toMovement)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> variantMovements(Long variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new NotFoundException("Variant " + variantId + " was not found");
        }
        return movementRepository.findTop100ByVariant_IdOrderByIdDesc(variantId).stream()
                .map(mapper::toMovement)
                .toList();
    }

    private void assertItemSellable(Item item) {
        if (!item.isActive()) {
            throw new BusinessRuleException("Item " + item.getSku() + " is not available for sale");
        }
        if (variantRepository.existsByItem_IdAndActiveTrue(item.getId())) {
            throw new BusinessRuleException(
                    "Item " + item.getSku() + " has variants. Sell a specific variant.");
        }
    }

    private void assertVariantSellable(Variant variant) {
        if (!variant.isActive() || !variant.getItem().isActive()) {
            throw new BusinessRuleException("Variant " + variant.getSku() + " is not available for sale");
        }
    }

    private StockMovement movement(
            Item item,
            Variant variant,
            MovementType type,
            int delta,
            int balance,
            String note,
            Sale sale
    ) {
        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setVariant(variant);
        movement.setType(type);
        movement.setQuantityDelta(delta);
        movement.setBalanceAfter(balance);
        movement.setNote(Text.trimToNull(note));
        movement.setSale(sale);
        return movement;
    }

    private void requireDelta(int delta) {
        if (delta == 0) {
            throw new BadRequestException("Quantity change cannot be zero");
        }
    }
}
