package com.jpmc.positionbook.controller;

import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.service.PositionBookService;
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

    @GetMapping
    public List<PositionResponse> getAllPositions() {
        return service.getAllPositions();
    }

    @GetMapping("/{account}/{securityId}")
    public PositionResponse getPosition(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]+$")
            @Size(min = 1, max = 50)
            String account,
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]+$")
            @Size(min = 1, max = 50)
            String securityId) {
        return service.getPosition(account, securityId);
    }
}
