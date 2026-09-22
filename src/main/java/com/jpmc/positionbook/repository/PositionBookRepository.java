package com.jpmc.positionbook.repository;

import com.jpmc.positionbook.exception.DuplicateEventException;
import com.jpmc.positionbook.exception.EventNotFoundException;
import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.EventType;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.TradeEventRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory store for accepted trade events and derived positions.
 *
 * <p>Concurrency strategy: mutating operations are guarded by {@code synchronized}
 * methods on this instance. A single lock is simple to reason about and correct at
 * the current scale, since every mutation touches at most a handful of maps and
 * completes quickly. The obvious next step as throughput grows is per-position lock
 * striping (e.g. a lock per {@link PositionKey}) so that unrelated positions stop
 * contending with each other on a single monitor — but that adds complexity (lock
 * management, avoiding deadlock across multi-position operations) that isn't
 * justified until contention on the single lock is actually observed.
 *
 * <p>Check-then-act collapsing: every mutating operation exposed here (duplicate-id
 * checks, cancellation-state checks, net quantity updates) is performed inside a
 * single synchronized method body rather than as separate "check" and "act" calls.
 * Exposing a public check method (e.g. {@code eventExists}) intended to be called
 * before a later, separate mutating call would not be atomic even if each method
 * were individually synchronized: another thread could interleave between the two
 * calls and invalidate the result of the check before the mutation runs. Collapsing
 * the full check-and-mutate sequence into one synchronized method removes that window
 * entirely.
 */
@Repository
public class PositionBookRepository {

    private final Map<Long, TradeEventRecord> eventsById = new ConcurrentHashMap<>();
    private final Set<Long> cancelledEventIds = ConcurrentHashMap.newKeySet();
    private final Map<PositionKey, Long> netQuantities = new ConcurrentHashMap<>();
    private final Map<PositionKey, List<TradeEventRecord>> drilldowns = new ConcurrentHashMap<>();

    /**
     * Atomically records a BUY or SELL event: checks for a duplicate id and, if none
     * exists, stores the event, appends it to its position's drilldown, and applies
     * {@code delta} to the position's net quantity — all inside one synchronized block.
     */
    public synchronized TradeEventRecord recordBuyOrSell(TradeEventRecord event, long delta) {
        if (eventsById.containsKey(event.getId())) {
            throw new DuplicateEventException("Event already exists with id: " + event.getId());
        }
        eventsById.put(event.getId(), event);
        PositionKey key = new PositionKey(event.getAccount(), event.getSecurityId());
        drilldownFor(key).add(event);
        netQuantities.merge(key, delta, Math::addExact);
        return event;
    }

    /**
     * Atomically cancels a previously recorded BUY or SELL event: looks up the
     * original event, verifies it hasn't already been cancelled, reverses its effect
     * on the net quantity, marks it cancelled, and appends a CANCEL record to the
     * position's drilldown — all inside one synchronized block.
     */
    public synchronized TradeEventRecord cancelEvent(Long cancelId) {
        TradeEventRecord original = eventsById.get(cancelId);
        if (original == null) {
            throw new EventNotFoundException("No event found with id: " + cancelId);
        }
        if (cancelledEventIds.contains(cancelId)) {
            throw new InvalidEventException("Event already cancelled: " + cancelId);
        }

        long reversal = original.getType() == EventType.BUY
                ? Math.negateExact(original.getQuantity())
                : original.getQuantity();

        PositionKey key = new PositionKey(original.getAccount(), original.getSecurityId());
        netQuantities.merge(key, reversal, Math::addExact);
        cancelledEventIds.add(cancelId);

        TradeEventRecord cancelRecord = new TradeEventRecord(
                original.getId(),
                EventType.CANCEL,
                original.getAccount(),
                original.getSecurityId(),
                0L);
        drilldownFor(key).add(cancelRecord);
        return cancelRecord;
    }

    /**
     * Read-only lookup. Must never be used by callers as the basis for a subsequent
     * mutating decision (e.g. "if findEvent(id) == null then recordBuyOrSell(...)") —
     * doing so reintroduces the exact check-then-act race that {@link #recordBuyOrSell}
     * and {@link #cancelEvent} are designed to avoid. Use those atomic methods instead.
     */
    public TradeEventRecord findEvent(Long id) {
        return eventsById.get(id);
    }

    /**
     * Read-only lookup. See {@link #findEvent(Long)} for the same caveat: never use
     * this as the basis for a subsequent mutating call.
     */
    public boolean isCancelled(Long id) {
        return cancelledEventIds.contains(id);
    }

    /**
     * Read-only lookup. See {@link #findEvent(Long)} for the same caveat: never use
     * this as the basis for a subsequent mutating call.
     */
    public boolean eventExists(Long id) {
        return eventsById.containsKey(id);
    }

    /**
     * Returns the position for the given account/securityId, or a zero-quantity,
     * empty-events position if nothing has ever traded for that key. Never returns
     * null or an empty Optional.
     *
     * <p>Assumes {@code account} and {@code securityId} have already been normalized
     * (trimmed/uppercased) by the caller — this repository does not re-normalize them.
     */
    public PositionResponse findPosition(String account, String securityId) {
        PositionKey key = new PositionKey(account, securityId);
        Long netQuantity = netQuantities.getOrDefault(key, 0L);
        List<TradeEventRecord> events = drilldowns.getOrDefault(key, List.of());
        return new PositionResponse(account, securityId, netQuantity, List.copyOf(events));
    }

    /**
     * Returns a defensive snapshot of all positions currently held. No returned list
     * or PositionResponse shares mutable state with this repository's internals.
     */
    public List<PositionResponse> findAllPositions() {
        List<PositionResponse> result = new ArrayList<>();
        for (Map.Entry<PositionKey, Long> entry : netQuantities.entrySet()) {
            PositionKey key = entry.getKey();
            List<TradeEventRecord> events = drilldowns.getOrDefault(key, List.of());
            result.add(new PositionResponse(key.getAccount(), key.getSecurityId(), entry.getValue(), List.copyOf(events)));
        }
        return Collections.unmodifiableList(result);
    }

    private List<TradeEventRecord> drilldownFor(PositionKey key) {
        return drilldowns.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
    }
}
