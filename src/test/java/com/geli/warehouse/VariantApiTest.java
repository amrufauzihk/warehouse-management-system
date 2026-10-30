package com.geli.warehouse;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VariantApiTest extends ApiTestSupport {

    @Test
    void requiresItemStockToBeClearedBeforeVariantsAndThenSellsTheVariant() throws Exception {
        long itemId = createItem(sku("tee"), "25.00", 6);

        mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
                        .contentType("application/json")
                        .content("""
                                {"sku":"%s","name":"Black / M","price":25.00,"initialStock":1}
                                """.formatted(sku("blk"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Adjust that stock to 0")));

        mockMvc.perform(post("/api/v1/items/" + itemId + "/stock-adjustments")
                        .contentType("application/json")
                        .content("""
                                {"quantityDelta":-6,"note":"move stock onto variants"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(0));

        String variantSku = sku("blk");
        long variantId = createVariant(itemId, variantSku, "25.00", 2);

        mockMvc.perform(get("/api/v1/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellableAsItem").value(false))
                .andExpect(jsonPath("$.variants[0].sku").value(variantSku.toUpperCase(Locale.ROOT)));

        mockMvc.perform(post("/api/v1/items/" + itemId + "/stock-adjustments")
                        .contentType("application/json")
                        .content("""
                                {"quantityDelta":1,"note":"should fail"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"itemId":%d,"quantity":1}]}
                                """.formatted(itemId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("has variants")));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":3}]}
                                """.formatted(variantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"))
                .andExpect(jsonPath("$.available").value(2))
                .andExpect(jsonPath("$.requested").value(3));

        mockMvc.perform(get("/api/v1/variants/" + variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(2));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"reference":"%s","lines":[{"variantId":%d,"quantity":2}]}
                                """.formatted(sku("pos"), variantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].balanceAfter").value(0))
                .andExpect(jsonPath("$.totalAmount").value(50.00));
    }

    @Test
    void rejectsSkuThatAlreadyBelongsToAnItem() throws Exception {
        String sku = sku("shared");
        long itemId = createItem(sku, "10.00", 0);

        mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
                        .contentType("application/json")
                        .content("""
                                {"sku":"%s","name":"Same","price":10.00,"initialStock":0}
                                """.formatted(sku)))
                .andExpect(status().isConflict());
    }

    @Test
    void priceChangeDoesNotRewriteAnEarlierSale() throws Exception {
        long itemId = createItem(sku("cap"), "10.00", 0);
        long variantId = createVariant(itemId, sku("capv"), "10.00", 1);

        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1}]}
                                """.formatted(variantId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long saleId = objectMapper.readTree(saleBody).get("id").asLong();

        mockMvc.perform(put("/api/v1/variants/" + variantId)
                        .contentType("application/json")
                        .content("""
                                {"name":"Cap / M","attributes":{"size":"M"},"price":18.00,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(18.00));

        mockMvc.perform(get("/api/v1/sales/" + saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].unitPrice").value(10.00))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(10.00));
    }
}
