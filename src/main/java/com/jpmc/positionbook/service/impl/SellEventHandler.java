package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.EventType;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.model.TradeEventRecord;
import com.jpmc.positionbook.repository.PositionBookRepository;
import com.jpmc.positionbook.service.EventHandler;
import com.jpmc.positionbook.util.IdentifierNormalizer;
import org.springframework.stereotype.Component;

@Component
public class SellEventHandler implements EventHandler<SellRequest> {

    private final PositionBookRepository repository;

    public SellEventHandler(PositionBookRepository repository) {
        this.repository = repository;
    }

    @Override
    public PositionKey handle(SellRequest request) {
        String account = IdentifierNormalizer.normalize(request.getAccount());
        String securityId = IdentifierNormalizer.normalize(request.getSecurityId());

        TradeEventRecord record = new TradeEventRecord(
                request.getId(),
                EventType.SELL,
                account,
                securityId,
                request.getQuantity());

        repository.recordBuyOrSell(record, -request.getQuantity());

        return new PositionKey(account, securityId);
    }
}
