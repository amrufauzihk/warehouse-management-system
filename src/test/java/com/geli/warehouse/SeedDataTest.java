package com.geli.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:warehouse-seed;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SeedDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededCatalogCanBeSoldUntilAVariantIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/items/sku/MUG-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellableAsItem").value(true))
                .andExpect(jsonPath("$.stockQuantity").value(40));

        mockMvc.perform(get("/api/v1/items/sku/TEE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellableAsItem").value(false))
                .andExpect(jsonPath("$.variants.length()").value(3));

        String white = mockMvc.perform(get("/api/v1/variants/sku/TEE-WHT-M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long whiteId = objectMapper.readTree(white).get("id").asLong();

        mockMvc.perform(post("/api/v1/sales")
                        .contentType("application/json")
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1}]}
                                """.formatted(whiteId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sku").value("TEE-WHT-M"))
                .andExpect(jsonPath("$.available").value(0));

        JsonNode lowStock = objectMapper.readTree(mockMvc.perform(get("/api/v1/inventory").param("maxStock", "0"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        boolean foundWhiteTee = false;
        for (JsonNode position : lowStock) {
            if ("TEE-WHT-M".equals(position.get("sku").asText())) {
                foundWhiteTee = position.get("stockQuantity").asInt() == 0;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(foundWhiteTee, "seeded white tee should be listed with zero stock");
    }
}
