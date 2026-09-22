package com.jpmc.positionbook.model;

import jakarta.validation.constraints.NotNull;

public class CancelRequest {

    @NotNull
    private Long id;

    public CancelRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
