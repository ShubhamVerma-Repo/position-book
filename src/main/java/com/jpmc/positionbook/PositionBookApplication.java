package com.jpmc.positionbook;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Position Book API",
        description = "An in-memory trade position book service exposing BUY/SELL/CANCEL event ingestion and position lookup over a REST API.",
        version = "1.0"
))
public class PositionBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(PositionBookApplication.class, args);
    }

}
