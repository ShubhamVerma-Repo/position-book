package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.repository.PositionBookRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionBookServiceImplTest {

    private final PositionBookRepository repository = new PositionBookRepository();
    private final PositionBookServiceImpl service = new PositionBookServiceImpl(
            new BuyEventHandler(repository),
            new SellEventHandler(repository),
            new CancelEventHandler(repository),
            repository);

    @Test
    void getPositionThrowsInvalidEventExceptionWhenAccountIsNull() {
        assertThatThrownBy(() -> service.getPosition(null, "SEC1"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void getPositionThrowsInvalidEventExceptionWhenAccountIsBlank() {
        assertThatThrownBy(() -> service.getPosition("   ", "SEC1"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void getPositionThrowsInvalidEventExceptionWhenSecurityIdIsNull() {
        assertThatThrownBy(() -> service.getPosition("ACC1", null))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void getPositionThrowsInvalidEventExceptionWhenSecurityIdIsBlank() {
        assertThatThrownBy(() -> service.getPosition("ACC1", "   "))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void getPositionNormalizesAndReturnsPositionForValidInputs() {
        BuyRequest buyRequest = new BuyRequest();
        buyRequest.setId(1L);
        buyRequest.setAccount("acc1");
        buyRequest.setSecurityId("sec1");
        buyRequest.setQuantity(100L);
        service.processBuy(buyRequest);

        PositionResponse position = service.getPosition("acc1", "sec1");

        assertThat(position.getAccount()).isEqualTo("ACC1");
        assertThat(position.getSecurityId()).isEqualTo("SEC1");
        assertThat(position.getNetQuantity()).isEqualTo(100L);
    }

    @Test
    void getAllPositionsDelegatesToRepository() {
        BuyRequest buyRequest = new BuyRequest();
        buyRequest.setId(1L);
        buyRequest.setAccount("acc1");
        buyRequest.setSecurityId("sec1");
        buyRequest.setQuantity(100L);
        service.processBuy(buyRequest);

        assertThat(service.getAllPositions()).hasSize(1);
    }
}
