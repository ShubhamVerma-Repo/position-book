package com.jpmc.positionbook.model;

import java.util.Objects;

public final class PositionKey {

    private final String account;
    private final String securityId;

    public PositionKey(String account, String securityId) {
        this.account = account;
        this.securityId = securityId;
    }

    public String getAccount() {
        return account;
    }

    public String getSecurityId() {
        return securityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PositionKey)) {
            return false;
        }
        PositionKey that = (PositionKey) o;
        return Objects.equals(account, that.account) && Objects.equals(securityId, that.securityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, securityId);
    }
}
