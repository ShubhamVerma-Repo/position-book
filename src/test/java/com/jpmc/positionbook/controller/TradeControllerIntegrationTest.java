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
    void validSellReturns201AndNegativeNetQuantity() throws Exception {
        String body = """
                {"id": 301, "account": "sellacc", "securityId": "sellsec", "quantity": 75}
                """;

        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netQuantity").value(-75));
    }

    @Test
    void duplicateBuyIdReturns409WithErrorResponseShape() throws Exception {
        String body = """
                {"id": 401, "account": "dupacc", "securityId": "dupsec", "quantity": 10}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void cancelOfUnknownIdReturns404WithErrorResponseShape() throws Exception {
        String body = """
                {"id": 999999}
                """;

        mockMvc.perform(post("/api/v1/trades/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void buyWithZeroIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": 0, "account": "acc1", "securityId": "sec1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void buyWithNegativeIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": -5, "account": "acc1", "securityId": "sec1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void sellWithZeroIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": 0, "account": "acc1", "securityId": "sec1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void sellWithNegativeIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": -5, "account": "acc1", "securityId": "sec1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void cancelWithZeroIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": 0}
                """;

        mockMvc.perform(post("/api/v1/trades/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void cancelWithNegativeIdReturns400ReferencingId() throws Exception {
        String body = """
                {"id": -5}
                """;

        mockMvc.perform(post("/api/v1/trades/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("id")));
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

    @Test
    void buyWithInvalidCharacterInAccountReturns400() throws Exception {
        String body = """
                {"id": 501, "account": "ACC!1", "securityId": "sec1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("account")));
    }

    @Test
    void sellWithInvalidCharacterInSecurityIdReturns400() throws Exception {
        String body = """
                {"id": 502, "account": "acc1", "securityId": "SEC!1", "quantity": 100}
                """;

        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("securityId")));
    }

    @Test
    void buyWithOverLengthAccountReturns400() throws Exception {
        String tooLong = "A".repeat(51);
        String body = """
                {"id": 503, "account": "%s", "securityId": "sec1", "quantity": 100}
                """.formatted(tooLong);

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("account")));
    }

    @Test
    void sellWithOverLengthSecurityIdReturns400() throws Exception {
        String tooLong = "S".repeat(51);
        String body = """
                {"id": 504, "account": "acc1", "securityId": "%s", "quantity": 100}
                """.formatted(tooLong);

        mockMvc.perform(post("/api/v1/trades/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("securityId")));
    }

    @Test
    void buyWithNonNumericQuantityReturns400WithErrorResponseShapeNotProblemDetail() throws Exception {
        String body = """
                {"id": 505, "account": "acc1", "securityId": "sec1", "quantity": "abc"}
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
    void positionFullyUnwoundToZeroStillAppearsInGetAllPositions() throws Exception {
        String buyBody = """
                {"id": 601, "account": "zeroacc", "securityId": "zerosec", "quantity": 50}
                """;
        String cancelBody = """
                {"id": 601}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/trades/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netQuantity").value(0));

        mockMvc.perform(get("/api/v1/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.account == 'ZEROACC' && @.securityId == 'ZEROSEC' && @.netQuantity == 0)]").exists());
    }

    @Test
    void validBuyWithReorderedJsonFieldsIsStillAccepted() throws Exception {
        String body = """
                {"quantity": 42, "securityId": "reordersec", "account": "reorderacc", "id": 701}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netQuantity").value(42));
    }

    @Test
    void buyWithFractionalQuantityReturns400WithErrorResponseShapeNotProblemDetail() throws Exception {
        String body = """
                {"id": 801, "account": "acc1", "securityId": "sec1", "quantity": 10.5}
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
    void buyWithFractionalIdReturns400WithErrorResponseShapeNotProblemDetail() throws Exception {
        String body = """
                {"id": 5.5, "account": "acc1", "securityId": "sec1", "quantity": 100}
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
    void buyWithWholeNumberWrittenInDecimalNotationForQuantityReturns400() throws Exception {
        String body = """
                {"id": 802, "account": "acc1", "securityId": "sec1", "quantity": 100.0}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void buyWithNegativeFractionalQuantityReturns400NotAnUnhandledException() throws Exception {
        String body = """
                {"id": 803, "account": "acc1", "securityId": "sec1", "quantity": -10.5}
                """;

        mockMvc.perform(post("/api/v1/trades/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
