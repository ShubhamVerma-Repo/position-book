package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.service.PositionBookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PositionBookServiceImplIntegrationTest {

    @Autowired
    private PositionBookService service;

    @Test
    void buyHandlerIsWiredToBuySlotAndIncreasesNetQuantity() {
        BuyRequest request = buyRequest(1L, "acc-buy", "sec-buy", 100L);

        PositionResponse position = service.processBuy(request);

        assertThat(position.getNetQuantity()).isEqualTo(100L);
    }

    @Test
    void sellHandlerIsWiredToSellSlotAndDecreasesNetQuantityIntoNegative() {
        SellRequest request = sellRequest(2L, "acc-sell", "sec-sell", 100L);

        PositionResponse position = service.processSell(request);

        // A fresh position starts at net 0. If the SELL slot were ever wired to
        // BuyEventHandler instead of SellEventHandler, this would come back as
        // +100, not -100 - which is exactly the swap this test is designed to catch.
        assertThat(position.getNetQuantity()).isEqualTo(-100L);
    }

    @Test
    void cancelHandlerIsWiredToCancelSlotAndReversesPriorBuyBackToZero() {
        BuyRequest buyRequest = buyRequest(3L, "acc-cancel", "sec-cancel", 100L);
        service.processBuy(buyRequest);

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setId(3L);
        PositionResponse position = service.processCancel(cancelRequest);

        assertThat(position.getNetQuantity()).isEqualTo(0L);
    }

    private BuyRequest buyRequest(Long id, String account, String securityId, Long quantity) {
        BuyRequest request = new BuyRequest();
        request.setId(id);
        request.setAccount(account);
        request.setSecurityId(securityId);
        request.setQuantity(quantity);
        return request;
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
