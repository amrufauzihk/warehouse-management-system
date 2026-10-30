package com.geli.warehouse.web;

import com.geli.warehouse.service.InventoryService;
import com.geli.warehouse.web.dto.InventoryPositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List sellable stock positions. Use maxStock to find low or empty stock.")
    public List<InventoryPositionResponse> list(@RequestParam(required = false) Integer maxStock) {
        return inventoryService.list(maxStock);
    }
}
