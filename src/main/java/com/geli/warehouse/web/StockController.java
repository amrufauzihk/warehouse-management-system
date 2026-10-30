package com.geli.warehouse.web;

import com.geli.warehouse.service.StockService;
import com.geli.warehouse.web.dto.StockAdjustmentRequest;
import com.geli.warehouse.web.dto.StockMovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/api/v1/items/{itemId}/stock-adjustments")
    @Operation(summary = "Receive or correct stock stored on an item that has no active variants")
    public StockMovementResponse adjustItem(
            @PathVariable Long itemId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return stockService.adjustItem(itemId, request.quantityDelta(), request.note());
    }

    @GetMapping("/api/v1/items/{itemId}/stock-movements")
    @Operation(summary = "Latest item-level stock movements")
    public List<StockMovementResponse> itemMovements(@PathVariable Long itemId) {
        return stockService.itemMovements(itemId);
    }

    @PostMapping("/api/v1/variants/{variantId}/stock-adjustments")
    @Operation(summary = "Receive or correct stock stored on a variant")
    public StockMovementResponse adjustVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        return stockService.adjustVariant(variantId, request.quantityDelta(), request.note());
    }

    @GetMapping("/api/v1/variants/{variantId}/stock-movements")
    @Operation(summary = "Latest variant stock movements")
    public List<StockMovementResponse> variantMovements(@PathVariable Long variantId) {
        return stockService.variantMovements(variantId);
    }
}
