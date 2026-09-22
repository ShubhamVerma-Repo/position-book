package com.jpmc.positionbook.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorResponseTest {

    @Test
    void nullDetailsDefaultsToEmptyImmutableList() {
        ErrorResponse response = new ErrorResponse(400, "Bad Request", "invalid", null);

        assertThat(response.getDetails()).isEmpty();
        assertThatThrownBy(() -> response.getDetails().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nonNullDetailsIsDefensivelyCopiedAndUnmodifiable() {
        List<String> mutableSource = new java.util.ArrayList<>(List.of("field: must not be null"));

        ErrorResponse response = new ErrorResponse(400, "Bad Request", "invalid", mutableSource);
        mutableSource.clear();

        assertThat(response.getDetails()).containsExactly("field: must not be null");
        assertThatThrownBy(() -> response.getDetails().add("y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fieldsAreSetFromConstructorAndTimestampIsAutoAssigned() {
        ErrorResponse response = new ErrorResponse(404, "Not Found", "missing", List.of());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("missing");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
