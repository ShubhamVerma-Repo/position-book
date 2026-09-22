package com.jpmc.positionbook.repository;

import com.jpmc.positionbook.exception.DuplicateEventException;
import com.jpmc.positionbook.exception.EventNotFoundException;
import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.EventType;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.TradeEventRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void concurrentRecordBuyOrSellWithSameIdAllowsExactlyOneWinnerUnderRealThreadContention() throws InterruptedException {
        int threadCount = 20;
        int iterations = 10;

        for (int iteration = 0; iteration < iterations; iteration++) {
            PositionBookRepository freshRepository = new PositionBookRepository();
            long eventId = iteration;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Class<? extends Throwable>> outcomes = new CopyOnWriteArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
                for (int t = 0; t < threadCount; t++) {
                    long quantity = 10L + t;
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            TradeEventRecord event = new TradeEventRecord(
                                    eventId, EventType.BUY, "RACEACC", "RACESEC", quantity);
                            freshRepository.recordBuyOrSell(event, quantity);
                            successCount.incrementAndGet();
                            outcomes.add(null);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Throwable thrown) {
                            outcomes.add(thrown.getClass());
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                startLatch.countDown();
                boolean completedInTime = doneLatch.await(10, TimeUnit.SECONDS);
                assertThat(completedInTime)
                        .as("iteration %d: all %d threads should finish within the timeout; a false result here "
                                        + "indicates a likely deadlock in recordBuyOrSell, not a slow environment",
                                iteration, threadCount)
                        .isTrue();
            } finally {
                executor.shutdown();
                boolean terminatedInTime = executor.awaitTermination(10, TimeUnit.SECONDS);
                assertThat(terminatedInTime)
                        .as("iteration %d: executor should terminate within the timeout; a false result here "
                                        + "indicates a likely deadlock in recordBuyOrSell",
                                iteration)
                        .isTrue();
            }

            assertThat(successCount.get())
                    .as("iteration %d: exactly one thread should win the duplicate-id race", iteration)
                    .isEqualTo(1);

            long duplicateExceptionCount = outcomes.stream()
                    .filter(outcome -> outcome != null)
                    .filter(DuplicateEventException.class::equals)
                    .count();
            long unexpectedExceptionCount = outcomes.stream()
                    .filter(outcome -> outcome != null)
                    .filter(outcome -> !DuplicateEventException.class.equals(outcome))
                    .count();

            assertThat(unexpectedExceptionCount)
                    .as("iteration %d: no thread should throw anything other than DuplicateEventException", iteration)
                    .isEqualTo(0);
            assertThat(duplicateExceptionCount)
                    .as("iteration %d: exactly threadCount-1 threads should lose the race with DuplicateEventException", iteration)
                    .isEqualTo(threadCount - 1);

            PositionResponse position = freshRepository.findPosition("RACEACC", "RACESEC");
            TradeEventRecord winningEvent = freshRepository.findEvent(eventId);
            assertThat(position.getNetQuantity())
                    .as("iteration %d: net quantity should reflect exactly one successful application", iteration)
                    .isEqualTo(winningEvent.getQuantity());
        }
    }

    @Test
    void concurrentRecordBuyOrSellWithDistinctIdsAppliesAllWritesExactlyOnceUnderRealThreadContention() throws InterruptedException {
        int threadCount = 50;
        int iterations = 10;

        for (int iteration = 0; iteration < iterations; iteration++) {
            PositionBookRepository freshRepository = new PositionBookRepository();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Class<? extends Throwable>> unexpectedOutcomes = new CopyOnWriteArrayList<>();
            long expectedTotal = 0;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
                for (int t = 0; t < threadCount; t++) {
                    long eventId = (long) (iteration * threadCount) + t;
                    long quantity = 1L + t;
                    expectedTotal += quantity;
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            TradeEventRecord event = new TradeEventRecord(
                                    eventId, EventType.BUY, "DISTINCTACC", "DISTINCTSEC", quantity);
                            freshRepository.recordBuyOrSell(event, quantity);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Throwable thrown) {
                            unexpectedOutcomes.add(thrown.getClass());
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                startLatch.countDown();
                boolean completedInTime = doneLatch.await(10, TimeUnit.SECONDS);
                assertThat(completedInTime)
                        .as("iteration %d: all %d threads should finish within the timeout; a false result here "
                                        + "indicates a likely deadlock in recordBuyOrSell, not a slow environment",
                                iteration, threadCount)
                        .isTrue();
            } finally {
                executor.shutdown();
                boolean terminatedInTime = executor.awaitTermination(10, TimeUnit.SECONDS);
                assertThat(terminatedInTime)
                        .as("iteration %d: executor should terminate within the timeout; a false result here "
                                        + "indicates a likely deadlock in recordBuyOrSell",
                                iteration)
                        .isTrue();
            }

            assertThat(unexpectedOutcomes)
                    .as("iteration %d: no thread using a distinct event id should ever throw", iteration)
                    .isEmpty();

            PositionResponse position = freshRepository.findPosition("DISTINCTACC", "DISTINCTSEC");
            assertThat(position.getNetQuantity())
                    .as("iteration %d: net quantity should equal the exact sum of all %d distinct writes", iteration, threadCount)
                    .isEqualTo(expectedTotal);
            assertThat(position.getEvents())
                    .as("iteration %d: every one of the %d distinct events should be recorded", iteration, threadCount)
                    .hasSize(threadCount);
        }
    }
}
