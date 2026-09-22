package com.jpmc.positionbook.repository;

import com.jpmc.positionbook.exception.DuplicateEventException;
import com.jpmc.positionbook.exception.EventNotFoundException;
import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.EventType;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.TradeEventRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionBookRepositoryTest {

    private final PositionBookRepository repository = new PositionBookRepository();

    @Test
    void recordBuyOrSellThrowsDuplicateEventExceptionForRepeatedId() {
        TradeEventRecord event = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        repository.recordBuyOrSell(event, 100L);

        TradeEventRecord duplicate = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 50L);
        assertThatThrownBy(() -> repository.recordBuyOrSell(duplicate, 50L))
                .isInstanceOf(DuplicateEventException.class);
    }

    @Test
    void cancelEventThrowsEventNotFoundExceptionForUnknownId() {
        assertThatThrownBy(() -> repository.cancelEvent(999L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void cancelEventThrowsInvalidEventExceptionOnSecondCancel() {
        TradeEventRecord event = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        repository.recordBuyOrSell(event, 100L);
        repository.cancelEvent(1L);

        assertThatThrownBy(() -> repository.cancelEvent(1L))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void cancellingABuyReversesNetQuantityNegatively() {
        TradeEventRecord event = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        repository.recordBuyOrSell(event, 100L);

        repository.cancelEvent(1L);

        PositionResponse position = repository.findPosition("ACC1", "SEC1");
        assertThat(position.getNetQuantity()).isEqualTo(0L);
    }

    @Test
    void cancellingASellReversesNetQuantityPositively() {
        // Exercises the "else" side of the BUY/SELL reversal ternary in cancelEvent -
        // a SELL's reversal must be added back (positive), not negated like a BUY's.
        TradeEventRecord event = new TradeEventRecord(1L, EventType.SELL, "ACC1", "SEC1", 100L);
        repository.recordBuyOrSell(event, -100L);

        TradeEventRecord cancelRecord = repository.cancelEvent(1L);

        PositionResponse position = repository.findPosition("ACC1", "SEC1");
        assertThat(position.getNetQuantity()).isEqualTo(0L);
        assertThat(cancelRecord.getType()).isEqualTo(EventType.CANCEL);
        assertThat(cancelRecord.getQuantity()).isEqualTo(0L);
    }

    @Test
    void findPositionForNeverTradedPairReturnsZeroQuantityAndEmptyEvents() {
        PositionResponse position = repository.findPosition("NEVER", "TRADED");

        assertThat(position.getNetQuantity()).isEqualTo(0L);
        assertThat(position.getEvents()).isEmpty();
    }

    @Test
    void findAllPositionsReturnsDefensiveCopiesNotLiveReferences() {
        TradeEventRecord event = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", 100L);
        repository.recordBuyOrSell(event, 100L);

        var allPositions = repository.findAllPositions();
        assertThatThrownBy(() -> allPositions.add(repository.findPosition("ACC2", "SEC2")))
                .isInstanceOf(UnsupportedOperationException.class);

        // Mutating repository state after the snapshot must not affect the already-returned list.
        repository.recordBuyOrSell(new TradeEventRecord(2L, EventType.BUY, "ACC2", "SEC2", 5L), 5L);
        assertThat(allPositions).hasSize(1);
    }

    @Test
    void recordBuyOrSellThrowsArithmeticExceptionOnNetQuantityOverflow() {
        // The public BuyRequest DTO caps quantity at 1_000_000_000 via @Max, so this
        // overflow guard can never be reached through the real HTTP API. Calling the
        // repository directly bypasses that DTO-level validation to prove the guard
        // itself - Math.addExact inside recordBuyOrSell - genuinely fires.
        TradeEventRecord first = new TradeEventRecord(1L, EventType.BUY, "ACC1", "SEC1", Long.MAX_VALUE);
        repository.recordBuyOrSell(first, Long.MAX_VALUE);

        TradeEventRecord second = new TradeEventRecord(2L, EventType.BUY, "ACC1", "SEC1", 1L);
        assertThatThrownBy(() -> repository.recordBuyOrSell(second, 1L))
                .isInstanceOf(ArithmeticException.class);
    }
}
