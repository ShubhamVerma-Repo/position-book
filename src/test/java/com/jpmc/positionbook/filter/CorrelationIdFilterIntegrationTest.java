package com.jpmc.positionbook.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customCorrelationIdIsEchoedBackUnchanged() throws Exception {
        String suppliedCorrelationId = "test-correlation-id-123";

        mockMvc.perform(get("/api/v1/positions").header(CorrelationIdFilter.CORRELATION_ID_HEADER, suppliedCorrelationId))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                        .isEqualTo(suppliedCorrelationId));
    }

    @Test
    void missingCorrelationIdStillGetsAGeneratedValueBack() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/positions"))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
    }
}
