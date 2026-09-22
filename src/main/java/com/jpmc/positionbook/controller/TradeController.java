package com.jpmc.positionbook.controller;

import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.service.PositionBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final PositionBookService service;

    public TradeController(PositionBookService service) {
        this.service = service;
    }

    @Operation(summary = "Record a BUY event", description = "Increases netQuantity for the account/security by the given amount.")
    @ApiResponse(responseCode = "201", description = "Position after the buy is applied")
    @ApiResponse(responseCode = "400", description = "Validation failure, malformed JSON, unknown JSON field, or unsupported media type (415)")
    @ApiResponse(responseCode = "409", description = "An event with this id already exists")
    @ApiResponse(responseCode = "405", description = "Wrong HTTP method used on this endpoint")
    @PostMapping(value = "/buy", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse buy(@Valid @RequestBody BuyRequest request) {
        return service.processBuy(request);
    }

    @Operation(summary = "Record a SELL event", description = "Decreases netQuantity for the account/security by the given amount. Selling beyond current holdings is permitted and results in a negative netQuantity (a short position).")
    @ApiResponse(responseCode = "201", description = "Position after the sell is applied")
    @ApiResponse(responseCode = "400", description = "Validation failure, malformed JSON, unknown JSON field, or unsupported media type (415)")
    @ApiResponse(responseCode = "409", description = "An event with this id already exists")
    @ApiResponse(responseCode = "405", description = "Wrong HTTP method used on this endpoint")
    @PostMapping(value = "/sell", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse sell(@Valid @RequestBody SellRequest request) {
        return service.processSell(request);
    }

    @Operation(summary = "Cancel a previously recorded BUY or SELL event", description = "Reverses the effect of the referenced event on the position. CANCEL is terminal: a second cancel of the same id returns 400.")
    @ApiResponse(responseCode = "201", description = "Position after the cancellation is applied")
    @ApiResponse(responseCode = "400", description = "Validation failure, malformed JSON, unsupported media type (415), or the event was already cancelled")
    @ApiResponse(responseCode = "404", description = "No event exists with the given id")
    @ApiResponse(responseCode = "405", description = "Wrong HTTP method used on this endpoint")
    @PostMapping(value = "/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse cancel(@Valid @RequestBody CancelRequest request) {
        return service.processCancel(request);
    }
}
