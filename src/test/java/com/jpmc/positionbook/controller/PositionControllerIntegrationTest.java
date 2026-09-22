package com.jpmc.positionbook.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PositionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidCharacterInPathReturns400WithErrorResponseShape() throws Exception {
        mockMvc.perform(get("/api/v1/positions/ACC_1/SEC1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.instance").doesNotExist());
    }

    @Test
    void neverTradedPairReturns200WithZeroQuantityAndEmptyEvents() throws Exception {
        mockMvc.perform(get("/api/v1/positions/NEVERTRADED/NEVERTRADEDSEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("NEVERTRADED"))
                .andExpect(jsonPath("$.securityId").value("NEVERTRADEDSEC"))
                .andExpect(jsonPath("$.netQuantity").value(0))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events").isEmpty());
    }
}
