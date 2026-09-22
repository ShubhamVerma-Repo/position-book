package com.jpmc.positionbook.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierNormalizerTest {

    @Test
    void nullInputReturnsNull() {
        assertThat(IdentifierNormalizer.normalize(null)).isNull();
    }

    @Test
    void trimsAndUppercasesNonNullInput() {
        assertThat(IdentifierNormalizer.normalize("  acc1  ")).isEqualTo("ACC1");
    }
}
