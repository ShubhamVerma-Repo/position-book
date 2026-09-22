package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.TradeEventRecord;
import com.jpmc.positionbook.repository.PositionBookRepository;
import com.jpmc.positionbook.service.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class CancelEventHandler implements EventHandler<CancelRequest> {

    private final PositionBookRepository repository;

    public CancelEventHandler(PositionBookRepository repository) {
        this.repository = repository;
    }

    @Override
    public PositionKey handle(CancelRequest request) {
        TradeEventRecord cancelRecord = repository.cancelEvent(request.getId());
        return new PositionKey(cancelRecord.getAccount(), cancelRecord.getSecurityId());
    }
}
