package com.geli.warehouse.config;

import com.geli.warehouse.repository.ItemRepository;
import com.geli.warehouse.service.ItemService;
import com.geli.warehouse.service.VariantService;
import com.geli.warehouse.web.dto.CreateItemRequest;
import com.geli.warehouse.web.dto.CreateVariantRequest;
import com.geli.warehouse.web.dto.ItemDetailResponse;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final VariantService variantService;

    public DataSeeder(ItemRepository itemRepository, ItemService itemService, VariantService variantService) {
        this.itemRepository = itemRepository;
        this.itemService = itemService;
        this.variantService = variantService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (itemRepository.count() > 0) {
            return;
        }
        itemService.create(new CreateItemRequest(
                "MUG-001",
                "Ceramic Mug",
                "12oz stoneware mug",
                new BigDecimal("12.50"),
                40
        ));
        itemService.create(new CreateItemRequest(
                "NOTE-001",
                "Notebook",
                "A5 lined notebook",
                new BigDecimal("4.50"),
                2
        ));

        ItemDetailResponse tee = itemService.create(new CreateItemRequest(
                "TEE-001",
                "Cotton T-Shirt",
                "Unisex cotton tee",
                new BigDecimal("25.00"),
                0
        ));
        variantService.create(tee.id(), new CreateVariantRequest(
                "TEE-BLK-S",
                "Black / S",
                Map.of("color", "Black", "size", "S"),
                new BigDecimal("25.00"),
                10
        ));
        variantService.create(tee.id(), new CreateVariantRequest(
                "TEE-BLK-M",
                "Black / M",
                Map.of("color", "Black", "size", "M"),
                new BigDecimal("25.00"),
                4
        ));
        variantService.create(tee.id(), new CreateVariantRequest(
                "TEE-WHT-M",
                "White / M",
                Map.of("color", "White", "size", "M"),
                new BigDecimal("27.00"),
                0
        ));
    }
}
