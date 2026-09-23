package com.jpmc.positionbook.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CancelRequest {

    @NotNull
    @Positive
    @Schema(minimum = "1")
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
