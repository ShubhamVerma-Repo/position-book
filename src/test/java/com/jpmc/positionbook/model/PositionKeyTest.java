package com.jpmc.positionbook.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionKeyTest {

    private final PositionKey base = new PositionKey("ACC1", "SEC1");

    @Test
    void isEqualToItself() {
        assertThat(base).isEqualTo(base);
    }

    @Test
    void isNotEqualToNull() {
        assertThat(base).isNotEqualTo(null);
    }

    @Test
    void isNotEqualToDifferentType() {
        assertThat(base).isNotEqualTo("not a PositionKey");
    }

    @Test
    void isEqualToAnotherKeyWithSameAccountAndSecurityId() {
        PositionKey same = new PositionKey("ACC1", "SEC1");
        assertThat(base).isEqualTo(same);
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    void isNotEqualWhenAccountDiffers() {
        PositionKey other = new PositionKey("ACC2", "SEC1");
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenSecurityIdDiffers() {
        PositionKey other = new PositionKey("ACC1", "SEC2");
        assertThat(base).isNotEqualTo(other);
    }
}
