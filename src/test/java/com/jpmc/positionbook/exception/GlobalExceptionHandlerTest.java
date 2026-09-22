package com.jpmc.positionbook.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PositionNotFoundException and the generic Exception fallback have no real call path
 * that reaches them today (see PositionBookServiceImpl's class Javadoc and
 * GlobalExceptionHandler's mapping table) - PositionNotFoundException is a deliberate,
 * currently-unused extension point, and nothing in the app throws an unmapped exception
 * type. Both handler methods are still real production code, so they're verified here by
 * direct invocation rather than via a contrived HTTP trigger.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePositionNotFoundReturns404WithReasonPhraseAndMessage() {
        ResponseEntity<ErrorResponse> response = handler.handlePositionNotFound(
                new PositionNotFoundException("No position for ACC1/SEC1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("No position for ACC1/SEC1");
    }

    @Test
    void handleGenericExceptionReturns500WithoutLeakingInternalDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("sensitive internal detail that must not leak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).doesNotContain("sensitive internal detail");
    }

    @Test
    void handleArithmeticExceptionReturns400WithOverflowMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleArithmeticException(
                new ArithmeticException("long overflow"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Resulting quantity would overflow");
    }
}
