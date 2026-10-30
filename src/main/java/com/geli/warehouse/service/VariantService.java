package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.Money;
import com.geli.warehouse.domain.MovementType;
import com.geli.warehouse.domain.StringMapConverter;
import com.geli.warehouse.domain.Variant;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.BusinessRuleException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.web.dto.CreateVariantRequest;
import com.geli.warehouse.web.dto.UpdateVariantRequest;
import com.geli.warehouse.web.dto.VariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VariantService {

    private static final int MAX_ATTRIBUTES = 10;
    private static final int MAX_JSON_LENGTH = 2000;

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final SkuService skuService;
    private final StockService stockService;
    private final WarehouseMapper mapper;

    public VariantService(
            ItemRepository itemRepository,
            VariantRepository variantRepository,
            SkuService skuService,
            StockService stockService,
            WarehouseMapper mapper
    ) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.skuService = skuService;
        this.stockService = stockService;
        this.mapper = mapper;
    }

    @Transactional
    public VariantResponse create(Long itemId, CreateVariantRequest request) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NotFoundException("Item " + itemId + " was not found"));
        if (item.getStockQuantity() > 0) {
            throw new BusinessRuleException(
                    "This item still has " + item.getStockQuantity()
                            + " units tracked on the item. Adjust that stock to 0 before adding variants.");
        }

        String sku = skuService.normalize(request.sku());
        skuService.reserve(sku);

        Variant variant = new Variant();
        variant.setItem(item);
        variant.setSku(sku);
        variant.setName(request.name().trim());
        variant.setAttributes(sanitize(request.attributes()));
        variant.setPrice(Money.scale(request.price()));
        variant.setActive(true);
        Variant saved = variantRepository.save(variant);

        if (request.initialStock() > 0) {
            stockService.applyToVariant(
                    saved.getId(), request.initialStock(), MovementType.RECEIPT, "Initial stock", null);
        }
        return get(saved.getId());
    }

    @Transactional
    public VariantResponse update(Long id, UpdateVariantRequest request) {
        Variant variant = variantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant " + id + " was not found"));
        variant.setName(request.name().trim());
        variant.setAttributes(sanitize(request.attributes()));
        variant.setPrice(Money.scale(request.price()));
        variant.setActive(request.active());
        return mapper.toVariant(variant);
    }

    @Transactional
    public void deactivate(Long id) {
        Variant variant = variantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant " + id + " was not found"));
        variant.setActive(false);
    }

    @Transactional(readOnly = true)
    public VariantResponse get(Long id) {
        return mapper.toVariant(find(id));
    }

    @Transactional(readOnly = true)
    public VariantResponse getBySku(String sku) {
        String normalized = skuService.normalize(sku);
        Variant variant = variantRepository.findBySku(normalized)
                .orElseThrow(() -> new NotFoundException("Variant " + normalized + " was not found"));
        return mapper.toVariant(variant);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listForItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new NotFoundException("Item " + itemId + " was not found");
        }
        return variantRepository.findByItem_IdOrderBySkuAsc(itemId).stream()
                .map(mapper::toVariant)
                .toList();
    }

    private Variant find(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant " + id + " was not found"));
    }

    private Map<String, String> sanitize(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return new LinkedHashMap<>();
        }
        if (input.size() > MAX_ATTRIBUTES) {
            throw new BadRequestException("A variant can have at most " + MAX_ATTRIBUTES + " attributes");
        }
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!key.matches("[A-Za-z0-9_\\- ]{1,40}")) {
                throw new BadRequestException(
                        "Attribute names must be 1-40 characters: letters, digits, spaces, underscores, or hyphens");
            }
            if (value.isEmpty() || value.length() > 80) {
                throw new BadRequestException("Attribute values must be 1-80 characters");
            }
            cleaned.put(key, value);
        }
        if (new StringMapConverter().convertToDatabaseColumn(cleaned).length() > MAX_JSON_LENGTH) {
            throw new BadRequestException("Variant attributes are too large");
        }
        return cleaned;
    }
}
