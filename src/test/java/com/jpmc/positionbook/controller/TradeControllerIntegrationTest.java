package com.jpmc.positionbook.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TradeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void buyWithMissingQuantityReturns400WithErrorResponseShapeNotProblemDetail() throws Exception {
        String body = """
                {"id": 1, "account": "acc1", "securityId": "sec1"}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
    void buyWithMalformedJsonReturns400WithErrorResponseShapeNotProblemDetail() throws Exception {
        String malformedBody = "{ not valid json ";

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
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
    void cancelWithUnknownFieldReturns400ProvingFailOnUnknownPropertiesIsEnforced() throws Exception {
        String bodyWithUnknownField = """
                {"id": 1, "quantity": 999}
                """;

        mockMvc.perform(post("/api/v1/trades/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithUnknownField))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void buyWithUnsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("id=1"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getOnBuyEndpointReturns405() throws Exception {
        mockMvc.perform(get("/api/v1/trades/buy"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void validBuyThenGetPositionReflectsTheTrade() throws Exception {
        String body = """
                {"id": 101, "account": "posacc", "securityId": "possec", "quantity": 250}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netQuantity").value(250));

        mockMvc.perform(get("/api/v1/positions/posacc/possec"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("POSACC"))
                .andExpect(jsonPath("$.securityId").value("POSSEC"))
                .andExpect(jsonPath("$.netQuantity").value(250))
                .andExpect(jsonPath("$.events", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.events[0].id").value(101))
                .andExpect(jsonPath("$.events[0].type").value("BUY"));
    }

    @Test
    void getAllPositionsIncludesTradedPositionAfterATrade() throws Exception {
        String body = """
                {"id": 202, "account": "listacc", "securityId": "listsec", "quantity": 10}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.account == 'LISTACC' && @.securityId == 'LISTSEC')]").exists());
    }
}
