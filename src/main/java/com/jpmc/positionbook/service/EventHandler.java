package com.jpmc.positionbook.service;

import com.jpmc.positionbook.model.PositionKey;

public interface EventHandler<T> {

    PositionKey handle(T request);
}
