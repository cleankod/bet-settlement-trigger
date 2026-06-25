package eu.cleankod.settlementtrigger.adapter.in.rest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

record PlaceBetRequest(
        @NotBlank String userId,
        @NotBlank String eventId,
        @NotBlank String eventMarketId,
        @NotBlank String selectedWinnerId,
        @NotNull @DecimalMin(value = "0.01", message = "betAmount must be greater than 0") BigDecimal betAmount
) {
}
