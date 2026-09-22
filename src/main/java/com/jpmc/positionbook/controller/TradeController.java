package com.jpmc.positionbook.controller;

import com.jpmc.positionbook.model.BuyRequest;
import com.jpmc.positionbook.model.CancelRequest;
import com.jpmc.positionbook.model.PositionResponse;
import com.jpmc.positionbook.model.SellRequest;
import com.jpmc.positionbook.service.PositionBookService;
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

    @PostMapping(value = "/buy", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse buy(@Valid @RequestBody BuyRequest request) {
        return service.processBuy(request);
    }

    @PostMapping(value = "/sell", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse sell(@Valid @RequestBody SellRequest request) {
        return service.processSell(request);
    }

    @PostMapping(value = "/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse cancel(@Valid @RequestBody CancelRequest request) {
        return service.processCancel(request);
    }
}
