package eu.cleankod.settlementtrigger.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;

record EventOutcomeRequest(
        @NotBlank String eventId,
        @NotBlank String eventName,
        @NotBlank String eventWinnerId
) {
}
