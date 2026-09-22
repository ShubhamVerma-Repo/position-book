package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.model.EventType;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.model.TradeEventRecord;
import com.jpmc.positionbook.repository.PositionBookRepository;
import com.jpmc.positionbook.service.EventHandler;
import com.jpmc.positionbook.util.IdentifierNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("sellEventHandler")
public class SellEventHandler implements EventHandler<SellRequest> {

    private static final Logger log = LoggerFactory.getLogger(SellEventHandler.class);

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

        log.info("Accepted SELL event id={}, account={}, securityId={}", request.getId(), account, securityId);

        return new PositionKey(account, securityId);
    }
}
