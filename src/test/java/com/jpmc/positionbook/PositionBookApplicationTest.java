package com.jpmc.positionbook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class PositionBookApplicationTest {

    @Test
    void mainStartsTheSpringApplicationContextWithoutThrowing() {
        assertThatCode(() -> PositionBookApplication.main(new String[] {"--server.port=0"}))
                .doesNotThrowAnyException();
    }
}
