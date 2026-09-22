package com.jpmc.positionbook.service.impl;

import com.jpmc.positionbook.exception.InvalidEventException;
import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionKey;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.repository.PositionBookRepository;
import com.jpmc.positionbook.service.EventHandler;
import com.jpmc.positionbook.service.PositionBookService;
import com.jpmc.positionbook.util.IdentifierNormalizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link #getPosition(String, String)} never throws {@link com.jpmc.positionbook.exception.PositionNotFoundException}
 * today - {@link PositionBookRepository#findPosition} always returns a zero-quantity,
 * empty-events position rather than signalling absence. The exception class and its
 * mapping in GlobalExceptionHandler are kept as a deliberate extension point for a
 * future strict-lookup variant of this method, not an oversight.
 */
@Service
public class PositionBookServiceImpl implements PositionBookService {

    private final EventHandler<BuyRequest> buyEventHandler;
    private final EventHandler<SellRequest> sellEventHandler;
    private final EventHandler<CancelRequest> cancelEventHandler;
    private final PositionBookRepository repository;

    public PositionBookServiceImpl(@Qualifier("buyEventHandler") EventHandler<BuyRequest> buyEventHandler,
                                    @Qualifier("sellEventHandler") EventHandler<SellRequest> sellEventHandler,
                                    @Qualifier("cancelEventHandler") EventHandler<CancelRequest> cancelEventHandler,
                                    PositionBookRepository repository) {
        this.buyEventHandler = buyEventHandler;
        this.sellEventHandler = sellEventHandler;
        this.cancelEventHandler = cancelEventHandler;
        this.repository = repository;
    }

    @Override
    public PositionResponse processBuy(BuyRequest request) {
        PositionKey key = buyEventHandler.handle(request);
        return repository.findPosition(key.getAccount(), key.getSecurityId());
    }

    @Override
    public PositionResponse processSell(SellRequest request) {
        PositionKey key = sellEventHandler.handle(request);
        return repository.findPosition(key.getAccount(), key.getSecurityId());
    }

    @Override
    public PositionResponse processCancel(CancelRequest request) {
        PositionKey key = cancelEventHandler.handle(request);
        return repository.findPosition(key.getAccount(), key.getSecurityId());
    }

    @Override
    public PositionResponse getPosition(String account, String securityId) {
        if (account == null || account.isBlank() || securityId == null || securityId.isBlank()) {
            throw new InvalidEventException("account and securityId must not be blank");
        }
        String normalizedAccount = IdentifierNormalizer.normalize(account);
        String normalizedSecurityId = IdentifierNormalizer.normalize(securityId);
        return repository.findPosition(normalizedAccount, normalizedSecurityId);
    }

    @Override
    public List<PositionResponse> getAllPositions() {
        return repository.findAllPositions();
    }
}
