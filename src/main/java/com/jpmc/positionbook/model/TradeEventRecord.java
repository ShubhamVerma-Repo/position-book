package com.jpmc.positionbook.model;

import java.time.Instant;
import java.util.Objects;

public final class TradeEventRecord {

    private final Long id;
    private final EventType type;
    private final String account;
    private final String securityId;
    private final Long quantity;
    private final Instant processedAt;

    public TradeEventRecord(Long id, EventType type, String account, String securityId, Long quantity) {
        this.id = id;
        this.type = type;
        this.account = account;
        this.securityId = securityId;
        this.quantity = quantity;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public String getAccount() {
        return account;
    }

    public String getSecurityId() {
        return securityId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TradeEventRecord)) {
            return false;
        }
        TradeEventRecord that = (TradeEventRecord) o;
        return Objects.equals(id, that.id)
                && type == that.type
                && Objects.equals(account, that.account)
                && Objects.equals(securityId, that.securityId)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(processedAt, that.processedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, account, securityId, quantity, processedAt);
    }

    @Override
    public String toString() {
        return "[id: " + id + ", " + type + ", " + account + ", " + securityId + ", " + quantity + "]";
    }
}
