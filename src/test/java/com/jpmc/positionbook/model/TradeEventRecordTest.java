package com.jpmc.positionbook.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventRecordTest {

    private final TradeEventRecord base = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);

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
        assertThat(base).isNotEqualTo("not a TradeEventRecord");
    }

    @Test
    void isEqualToAnotherRecordWithAllSameFieldsIncludingProcessedAt() throws Exception {
        // processedAt is always freshly stamped by the constructor, so two independently
        // constructed records can never naturally share it. Reflection aligns that one
        // field so the fully-equal path through equals()/hashCode() is genuinely exercised.
        TradeEventRecord same = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        Instant sharedInstant = base.getProcessedAt();
        setProcessedAt(same, sharedInstant);

        assertThat(base).isEqualTo(same);
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
    }

    private void setProcessedAt(TradeEventRecord record, Instant instant) throws Exception {
        Field field = TradeEventRecord.class.getDeclaredField("processedAt");
        field.setAccessible(true);
        field.set(record, instant);
    }

    @Test
    void isNotEqualWhenIdDiffers() {
        TradeEventRecord other = new TradeEventRecord(2L, EventType.BUY, "ACC1", "SEC1", 100L);
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenTypeDiffers() {
        TradeEventRecord other = new TradeEventRecord(1L, EventType.SELL, "ACC1", "SEC1", 100L);
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenAccountDiffers() {
        TradeEventRecord other = new TradeEventRecord(1L, EventType.BUY, "ACC2", "SEC1", 100L);
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenSecurityIdDiffers() {
        TradeEventRecord other = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC2", 100L);
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenQuantityDiffers() {
        TradeEventRecord other = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 200L);
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void isNotEqualWhenProcessedAtDiffers() throws Exception {
        TradeEventRecord other = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        setProcessedAt(other, base.getProcessedAt().plusSeconds(60));
        assertThat(base).isNotEqualTo(other);
    }

    @Test
    void toStringMatchesExactRequiredFormatAndExcludesProcessedAt() {
        TradeEventRecord record = new TradeEventRecord(42L, EventType.SELL, "ACC9", "SEC9", 500L);
        assertThat(record.toString()).isEqualTo("[id: 42, SELL, ACC9, SEC9, 500]");
    }
}
