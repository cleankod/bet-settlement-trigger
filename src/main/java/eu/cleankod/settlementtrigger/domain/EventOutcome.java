package eu.cleankod.settlementtrigger.domain;

public record EventOutcome(
        String eventId,
        String eventName,
        String eventWinnerId
) {
}
