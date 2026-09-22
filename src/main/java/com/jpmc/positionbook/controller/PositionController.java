package com.jpmc.positionbook.controller;

import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.service.PositionBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
@Validated
public class PositionController {

    private final PositionBookService service;

    public PositionController(PositionBookService service) {
        this.service = service;
    }

    @Operation(summary = "List every position currently tracked")
    @ApiResponse(responseCode = "200", description = "All positions, possibly empty")
    @GetMapping
    public List<PositionResponse> getAllPositions() {
        return service.getAllPositions();
    }

    @Operation(summary = "Get the position for a single account/security pair",
            description = "Always returns 200 with netQuantity 0 and an empty events list for an account/security pair that has never traded, rather than 404.")
    @ApiResponse(responseCode = "200", description = "The position, or a zero-state position if never traded")
    @ApiResponse(responseCode = "400", description = "account or securityId failed the alphanumeric/length constraint")
    @GetMapping("/{account}/{securityId}")
    public PositionResponse getPosition(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]+$")
            @Size(min = 1, max = 50)
            @Parameter(description = "Alphanumeric only, 1-50 characters")
            String account,
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]+$")
            @Size(min = 1, max = 50)
            @Parameter(description = "Alphanumeric only, 1-50 characters")
            String securityId) {
        return service.getPosition(account, securityId);
    }
}
