package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.Money;
import com.geli.warehouse.domain.MovementType;
import com.geli.warehouse.domain.Text;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.web.dto.CreateItemRequest;
import com.geli.warehouse.web.dto.ItemDetailResponse;
import com.geli.warehouse.web.dto.ItemSummaryResponse;
import com.geli.warehouse.web.dto.PageResponse;
import com.geli.warehouse.web.dto.UpdateItemRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final SkuService skuService;
    private final StockService stockService;
    private final WarehouseMapper mapper;

    public ItemService(
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
    public ItemDetailResponse create(CreateItemRequest request) {
        String sku = skuService.normalize(request.sku());
        skuService.reserve(sku);

        Item item = new Item();
        item.setSku(sku);
        item.setName(request.name().trim());
        item.setDescription(Text.trimToNull(request.description()));
        item.setPrice(Money.scale(request.price()));
        item.setActive(true);
        Item saved = itemRepository.save(item);

        if (request.initialStock() > 0) {
            stockService.applyToItem(saved.getId(), request.initialStock(), MovementType.RECEIPT, "Initial stock", null);
        }
        return get(saved.getId());
    }

    @Transactional
    public ItemDetailResponse update(Long id, UpdateItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " was not found"));
        item.setName(request.name().trim());
        item.setDescription(Text.trimToNull(request.description()));
        item.setPrice(Money.scale(request.price()));
        item.setActive(request.active());
        return get(id);
    }

    @Transactional
    public void deactivate(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " was not found"));
        item.setActive(false);
    }

    @Transactional(readOnly = true)
    public ItemDetailResponse get(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " was not found"));
        return mapper.toDetail(item, variantRepository.findByItem_IdOrderBySkuAsc(id));
    }

    @Transactional(readOnly = true)
    public ItemDetailResponse getBySku(String sku) {
        String normalized = skuService.normalize(sku);
        Item item = itemRepository.findBySku(normalized)
                .orElseThrow(() -> new NotFoundException("Item " + normalized + " was not found"));
        return get(item.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemSummaryResponse> list(String query, Boolean active, Pageable pageable) {
        Page<Item> page = itemRepository.findAll(filter(query, active), pageable);
        Map<Long, long[]> counts = summarize(page.getContent().stream().map(Item::getId).toList());
        List<ItemSummaryResponse> content = page.getContent().stream()
                .map(item -> {
                    long[] summary = counts.getOrDefault(item.getId(), new long[]{0, 0});
                    return mapper.toSummary(item, summary[0], summary[1]);
                })
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Map<Long, long[]> summarize(Collection<Long> ids) {
        Map<Long, long[]> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : variantRepository.summarizeByItemIds(ids)) {
            long itemId = ((Number) row[0]).longValue();
            long total = row[1] == null ? 0 : ((Number) row[1]).longValue();
            long active = row[2] == null ? 0 : ((Number) row[2]).longValue();
            counts.put(itemId, new long[]{total, active});
        }
        return counts;
    }

    private Specification<Item> filter(String query, Boolean active) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like)
                ));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
