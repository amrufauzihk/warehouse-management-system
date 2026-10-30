package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.MovementType;
import com.geli.warehouse.domain.Sale;
import com.geli.warehouse.domain.SaleLine;
import com.geli.warehouse.domain.StockMovement;
import com.geli.warehouse.domain.Variant;
import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.DuplicateReferenceException;
import com.geli.warehouse.exception.NotFoundException;
import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.repository.SaleLineRepository;
import com.geli.warehouse.repository.SaleRepository;
import com.geli.warehouse.repository.VariantRepository;
import com.geli.warehouse.web.dto.CreateSaleRequest;
import com.geli.warehouse.web.dto.PageResponse;
import com.geli.warehouse.web.dto.SaleLineRequest;
import com.geli.warehouse.web.dto.SaleResponse;
import com.geli.warehouse.web.dto.SaleSummaryResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class SaleService {

    private static final int MAX_LINE_QUANTITY = 10_000;

    private final SaleRepository saleRepository;
    private final SaleLineRepository saleLineRepository;
    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final StockService stockService;
    private final SkuService skuService;
    private final WarehouseMapper mapper;

    public SaleService(
            SaleRepository saleRepository,
            SaleLineRepository saleLineRepository,
            ItemRepository itemRepository,
            VariantRepository variantRepository,
            StockService stockService,
            SkuService skuService,
            WarehouseMapper mapper
    ) {
        this.saleRepository = saleRepository;
        this.saleLineRepository = saleLineRepository;
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
        this.skuService = skuService;
        this.mapper = mapper;
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request) {
        String reference = resolveReference(request.reference());
        if (saleRepository.existsByReference(reference)) {
            throw new DuplicateReferenceException(reference);
        }

        List<LineSpec> itemLines = new ArrayList<>();
        List<LineSpec> variantLines = new ArrayList<>();
        merge(request.lines(), itemLines, variantLines);

        Map<Long, Long> parentByVariant = new HashMap<>();
        for (LineSpec line : variantLines) {
            Variant variant = variantRepository.findById(line.targetId())
                    .orElseThrow(() -> new NotFoundException("Variant " + line.targetId() + " was not found"));
            parentByVariant.put(variant.getId(), variant.getItem().getId());
        }

        // Lock parents before variants so a sale and a variant update cannot deadlock.
        TreeSet<Long> itemIds = new TreeSet<>();
        itemLines.forEach(line -> itemIds.add(line.targetId()));
        itemIds.addAll(parentByVariant.values());
        for (Long itemId : itemIds) {
            itemRepository.findByIdForUpdate(itemId)
                    .orElseThrow(() -> new NotFoundException("Item " + itemId + " was not found"));
        }
        for (LineSpec line : variantLines) {
            variantRepository.findByIdForUpdate(line.targetId())
                    .orElseThrow(() -> new NotFoundException("Variant " + line.targetId() + " was not found"));
        }

        Sale sale = new Sale();
        sale.setReference(reference);
        sale.setTotalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        try {
            sale = saleRepository.saveAndFlush(sale);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateReferenceException(reference);
        }

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (LineSpec line : itemLines) {
            StockMovement movement = stockService.applyToItem(
                    line.targetId(), -line.quantity(), MovementType.SALE, "Sale " + reference, sale);
            Item item = itemRepository.findById(line.targetId()).orElseThrow();
            total = total.add(addLine(
                    sale, item, null, item.getSku(), item.getName(), item.getPrice(), line.quantity(), movement.getBalanceAfter()));
        }
        for (LineSpec line : variantLines) {
            StockMovement movement = stockService.applyToVariant(
                    line.targetId(), -line.quantity(), MovementType.SALE, "Sale " + reference, sale);
            Variant variant = variantRepository.findById(line.targetId()).orElseThrow();
            total = total.add(addLine(
                    sale,
                    variant.getItem(),
                    variant,
                    variant.getSku(),
                    variant.getName(),
                    variant.getPrice(),
                    line.quantity(),
                    movement.getBalanceAfter()));
        }
        sale.setTotalAmount(total);
        saleRepository.saveAndFlush(sale);
        return mapper.toSale(saleRepository.findWithLinesById(sale.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        return mapper.toSale(saleRepository.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("Sale " + id + " was not found")));
    }

    @Transactional(readOnly = true)
    public SaleResponse getByReference(String reference) {
        String normalized = skuService.normalizeReference(reference);
        if (normalized == null) {
            throw new BadRequestException("Sale reference is required");
        }
        return mapper.toSale(saleRepository.findWithLinesByReference(normalized)
                .orElseThrow(() -> new NotFoundException("Sale " + normalized + " was not found")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> list(Pageable pageable) {
        Page<Sale> page = saleRepository.findAll(pageable);
        Map<Long, Integer> counts = lineCounts(page.getContent().stream().map(Sale::getId).toList());
        List<SaleSummaryResponse> content = page.getContent().stream()
                .map(sale -> mapper.toSaleSummary(sale, counts.getOrDefault(sale.getId(), 0)))
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Map<Long, Integer> lineCounts(Collection<Long> ids) {
        Map<Long, Integer> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : saleLineRepository.countBySaleIds(ids)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return counts;
    }

    private void merge(List<SaleLineRequest> lines, List<LineSpec> itemLines, List<LineSpec> variantLines) {
        Map<Long, Integer> items = new HashMap<>();
        Map<Long, Integer> variants = new HashMap<>();
        for (SaleLineRequest line : lines) {
            try {
                if (line.variantId() != null) {
                    variants.merge(line.variantId(), line.quantity(), Math::addExact);
                } else {
                    items.merge(line.itemId(), line.quantity(), Math::addExact);
                }
            } catch (ArithmeticException ex) {
                throw new BadRequestException("Quantity is too large");
            }
        }
        items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> itemLines.add(checked(entry.getKey(), entry.getValue())));
        variants.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> variantLines.add(checked(entry.getKey(), entry.getValue())));
    }

    private LineSpec checked(Long id, int quantity) {
        if (quantity > MAX_LINE_QUANTITY) {
            throw new BadRequestException("Quantity per item cannot exceed " + MAX_LINE_QUANTITY);
        }
        return new LineSpec(id, quantity);
    }

    private BigDecimal addLine(
            Sale sale,
            Item item,
            Variant variant,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantity,
            int balanceAfter
    ) {
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        SaleLine line = new SaleLine();
        line.setItem(item);
        line.setVariant(variant);
        line.setSku(sku);
        line.setName(name);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setLineTotal(lineTotal);
        line.setBalanceAfter(balanceAfter);
        sale.addLine(line);
        return lineTotal;
    }

    private String resolveReference(String raw) {
        String normalized = skuService.normalizeReference(raw);
        if (normalized != null) {
            return normalized;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String generated = "SAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            if (!saleRepository.existsByReference(generated)) {
                return generated;
            }
        }
        throw new IllegalStateException("Could not allocate a sale reference");
    }

    private record LineSpec(Long targetId, int quantity) {
    }
}
