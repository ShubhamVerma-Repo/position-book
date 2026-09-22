package com.jpmc.positionbook.service;

import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;

import java.util.List;

public interface PositionBookService {

    PositionResponse processBuy(BuyRequest request);

    PositionResponse processSell(SellRequest request);

    PositionResponse processCancel(CancelRequest request);

    PositionResponse getPosition(String account, String securityId);

    List<PositionResponse> getAllPositions();
}
