package com.geli.warehouse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockApiTest extends ApiTestSupport {

    @Test
    void adjustmentUpdatesTheBalanceAndTheLedger() throws Exception {
        long itemId = createItem(sku("box"), "8.00", 5);

        mockMvc.perform(post("/api/v1/items/" + itemId + "/stock-adjustments")
                        .contentType("application/json")
                        .content("""
                                {"quantityDelta":2,"note":"found in the back"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantityDelta").value(2))
                .andExpect(jsonPath("$.balanceAfter").value(7));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":2}]}
                                """.formatted(itemId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/items/" + itemId + "/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SALE"))
                .andExpect(jsonPath("$[0].quantityDelta").value(-2))
                .andExpect(jsonPath("$[0].balanceAfter").value(5))
                .andExpect(jsonPath("$[1].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[2].type").value("RECEIPT"))
                .andExpect(jsonPath("$[2].quantityDelta").value(5));
    }

    @Test
    void adjustmentCannotDriveStockBelowZero() throws Exception {
        long itemId = createItem(sku("pen"), "1.50", 2);

        mockMvc.perform(post("/api/v1/items/" + itemId + "/stock-adjustments")
                        .contentType("application/json")
                        .content("""
                                {"quantityDelta":-5,"note":"damaged"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"))
                .andExpect(jsonPath("$.available").value(2));

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(2));

        mockMvc.perform(post("/api/v1/items/" + itemId + "/stock-adjustments")
                        .contentType("application/json")
                        .content("""
                                {"quantityDelta":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("validation")));
    }
}
