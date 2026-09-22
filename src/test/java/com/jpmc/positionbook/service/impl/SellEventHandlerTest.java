package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.repository.PositionBookRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SellEventHandlerTest {

    private final PositionBookRepository repository = new PositionBookRepository();
    private final SellEventHandler handler = new SellEventHandler(repository);

    @Test
    void allowsSellingBeyondCurrentHoldingsResultingInNegativeNetQuantity() {
        SellRequest request = sellRequest(1L, "acc1", "sec1", 100L);

        PositionKey key = handler.handle(request);

        PositionResponse position = repository.findPosition(key.getAccount(), key.getSecurityId());
        assertThat(position.getNetQuantity()).isEqualTo(-100L);
    }

    private SellRequest sellRequest(Long id, String account, String securityId, Long quantity) {
        SellRequest request = new SellRequest();
        request.setId(id);
        request.setAccount(account);
        request.setSecurityId(securityId);
        request.setQuantity(quantity);
        return request;
    }
}
