package com.jpmc.positionbook.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionResponseTest {

    @Test
    void nullEventsListDefaultsToEmptyImmutableList() {
        PositionResponse response = new PositionResponse("ACC1", "SEC1", 0L, null);

        assertThat(response.getEvents()).isEmpty();
        assertThatThrownBy(() -> response.getEvents().add(
                new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 1L)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nonNullEventsListIsDefensivelyCopiedAndUnmodifiable() {
        TradeEventRecord event = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        List<TradeEventRecord> mutableSource = new java.util.ArrayList<>(List.of(event));

        PositionResponse response = new PositionResponse("ACC1", "SEC1", 100L, mutableSource);
        mutableSource.clear();

        assertThat(response.getEvents()).containsExactly(event);
        assertThatThrownBy(() -> response.getEvents().add(event))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
