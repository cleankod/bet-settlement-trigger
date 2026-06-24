package eu.cleankod.settlementtrigger.domain;

public record EventOutcome(
        String eventId,
        String eventWinnerId
) {
}
