package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.exception.DuplicateEventException;
import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.repository.PositionBookRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuyEventHandlerTest {

    private final PositionBookRepository repository = new PositionBookRepository();
    private final BuyEventHandler handler = new BuyEventHandler(repository);

    @Test
    void normalizesAccountAndSecurityIdOnHandle() {
        BuyRequest request = buyRequest(1L, "acc1", "sec1", 100L);

        PositionKey key = handler.handle(request);

        assertThat(key.getAccount()).isEqualTo("ACC1");
        assertThat(key.getSecurityId()).isEqualTo("SEC1");
    }

    @Test
    void propagatesDuplicateEventExceptionOnDuplicateId() {
        BuyRequest first = buyRequest(1L, "acc1", "sec1", 100L);
        BuyRequest duplicate = buyRequest(1L, "acc1", "sec1", 50L);

        handler.handle(first);

        assertThatThrownBy(() -> handler.handle(duplicate))
                .isInstanceOf(DuplicateEventException.class);
    }

    private BuyRequest buyRequest(Long id, String account, String securityId, Long quantity) {
        BuyRequest request = new BuyRequest();
        request.setId(id);
        request.setAccount(account);
        request.setSecurityId(securityId);
        request.setQuantity(quantity);
        return request;
    }
}
