package com.jpmc.positionbook.model;

import java.util.List;

public final class PositionResponse {

    private final String account;
    private final String securityId;
    private final Long netQuantity;
    private final List<TradeEventRecord> events;

    public PositionResponse(String account, String securityId, Long netQuantity, List<TradeEventRecord> events) {
        this.account = account;
        this.securityId = securityId;
        this.netQuantity = netQuantity;
        this.events = events == null ? List.of() : List.copyOf(events);
    }

    public String getAccount() {
        return account;
    }

    public String getSecurityId() {
        return securityId;
    }

    public Long getNetQuantity() {
        return netQuantity;
    }

    public List<TradeEventRecord> getEvents() {
        return events;
    }
}
