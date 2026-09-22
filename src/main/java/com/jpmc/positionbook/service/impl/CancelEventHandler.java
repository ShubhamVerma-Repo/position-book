package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.TradeEventRecord;
import com.jpmc.positionbook.repository.PositionBookRepository;
import com.jpmc.positionbook.service.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("cancelEventHandler")
public class CancelEventHandler implements EventHandler<CancelRequest> {

    private static final Logger log = LoggerFactory.getLogger(CancelEventHandler.class);

    private final PositionBookRepository repository;

    public CancelEventHandler(PositionBookRepository repository) {
        this.repository = repository;
    }

    @Override
    public PositionKey handle(CancelRequest request) {
        TradeEventRecord cancelRecord = repository.cancelEvent(request.getId());

        log.info("Accepted CANCEL event id={}, account={}, securityId={}",
                cancelRecord.getId(), cancelRecord.getAccount(), cancelRecord.getSecurityId());

        return new PositionKey(cancelRecord.getAccount(), cancelRecord.getSecurityId());
    }
}
