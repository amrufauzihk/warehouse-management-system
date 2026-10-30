package com.geli.warehouse.service;

import com.geli.warehouse.domain.Item;
import com.geli.warehouse.domain.Sale;
import com.geli.warehouse.domain.SaleLine;
import com.geli.warehouse.domain.StockMovement;
import com.geli.warehouse.domain.Variant;
import com.geli.warehouse.web.dto.ItemDetailResponse;
import com.geli.warehouse.web.dto.ItemSummaryResponse;
import com.geli.warehouse.web.dto.SaleLineResponse;
import com.geli.warehouse.web.dto.SaleResponse;
import com.geli.warehouse.web.dto.SaleSummaryResponse;
import com.geli.warehouse.web.dto.StockMovementResponse;
import com.geli.warehouse.web.dto.VariantResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class WarehouseMapper {

    public ItemSummaryResponse toSummary(Item item, long variantCount, long activeVariantCount) {
        return new ItemSummaryResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getPrice(),
                item.getStockQuantity(),
                item.isActive(),
                (int) variantCount,
                item.isActive() && activeVariantCount == 0,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public ItemDetailResponse toDetail(Item item, List<Variant> variants) {
        boolean sellableAsItem = item.isActive() && variants.stream().noneMatch(Variant::isActive);
        return new ItemDetailResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getStockQuantity(),
                item.isActive(),
                sellableAsItem,
                variants.stream().map(this::toVariant).toList(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public VariantResponse toVariant(Variant variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getItem().getId(),
                variant.getSku(),
                variant.getName(),
                copyAttributes(variant),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.isActive(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }

    public StockMovementResponse toMovement(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getType(),
                movement.getQuantityDelta(),
                movement.getBalanceAfter(),
                movement.getNote(),
                movement.getSale() == null ? null : movement.getSale().getId(),
                movement.getCreatedAt()
        );
    }

    public SaleSummaryResponse toSaleSummary(Sale sale, int lineCount) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getReference(),
                sale.getTotalAmount(),
                lineCount,
                sale.getCreatedAt()
        );
    }

    public SaleResponse toSale(Sale sale) {
        List<SaleLineResponse> lines = sale.getLines().stream()
                .sorted(Comparator.comparing(SaleLine::getId))
                .map(this::toSaleLine)
                .toList();
        return new SaleResponse(
                sale.getId(),
                sale.getReference(),
                sale.getTotalAmount(),
                lines,
                sale.getCreatedAt()
        );
    }

    private Map<String, String> copyAttributes(Variant variant) {
        if (variant.getAttributes() == null || variant.getAttributes().isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(variant.getAttributes());
    }

    private SaleLineResponse toSaleLine(SaleLine line) {
        return new SaleLineResponse(
                line.getId(),
                line.getItem() == null ? null : line.getItem().getId(),
                line.getVariant() == null ? null : line.getVariant().getId(),
                line.getSku(),
                line.getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal(),
                line.getBalanceAfter()
        );
    }
}
