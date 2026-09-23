package com.jpmc.positionbook.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class BuyRequest {

    @NotNull
    @Positive
    @Schema(minimum = "1")
    private Long id;

    @NotBlank
    private String account;

    @NotBlank
    private String securityId;

    @NotNull
    @Positive
    @Max(1_000_000_000L)
    @Schema(minimum = "1", maximum = "1000000000")
    private Long quantity;

    public BuyRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
