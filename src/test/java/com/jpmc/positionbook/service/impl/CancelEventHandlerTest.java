package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.exception.EventNotFoundException;
import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.repository.PositionBookRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelEventHandlerTest {

    private final PositionBookRepository repository = new PositionBookRepository();
    private final BuyEventHandler buyHandler = new BuyEventHandler(repository);
    private final CancelEventHandler cancelHandler = new CancelEventHandler(repository);

    @Test
    void doubleCancelOfSameEventIsRejectedWithInvalidEventException() {
        // Cancelling the same BUY/SELL id twice: eventsById still holds the original
        // event (it is never removed), but cancelledEventIds already contains this id,
        // so the second attempt is rejected as an invalid state transition, not a
        // missing-event lookup failure.
        BuyRequest buyRequest = buyRequest(1L, "acc1", "sec1", 100L);
        buyHandler.handle(buyRequest);
        cancelHandler.handle(cancelRequest(1L));

        assertThatThrownBy(() -> cancelHandler.handle(cancelRequest(1L)))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void cancellingAnIdThatWasNeverRecordedIsRejectedWithEventNotFoundException() {
        // CANCEL records are never stored in eventsById under their own id - they
        // reuse the original BUY/SELL's id and live only in the drilldown. So there
        // is no such thing as a distinct "id belonging to a CANCEL record" to target;
        // the only way to hit a missing-lookup failure is to cancel an id that was
        // never a BUY/SELL at all.
        assertThatThrownBy(() -> cancelHandler.handle(cancelRequest(999L)))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void returnedPositionKeyMatchesOriginalEventNormalizedAccountAndSecurityIdNotCancelRequest() {
        BuyRequest buyRequest = buyRequest(1L, "acc1", "sec1", 100L);
        buyHandler.handle(buyRequest);

        PositionKey key = cancelHandler.handle(cancelRequest(1L));

        assertThat(key.getAccount()).isEqualTo("ACC1");
        assertThat(key.getSecurityId()).isEqualTo("SEC1");
    }

    private BuyRequest buyRequest(Long id, String account, String securityId, Long quantity) {
        BuyRequest request = new BuyRequest();
        request.setId(id);
        request.setAccount(account);
        request.setSecurityId(securityId);
        request.setQuantity(quantity);
        return request;
    }

    private CancelRequest cancelRequest(Long id) {
        CancelRequest request = new CancelRequest();
        request.setId(id);
        return request;
    }
}
