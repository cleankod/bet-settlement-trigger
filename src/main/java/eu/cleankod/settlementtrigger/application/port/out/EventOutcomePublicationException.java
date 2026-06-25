package eu.cleankod.settlementtrigger.application.port.out;

public class EventOutcomePublicationException extends RuntimeException {

    public EventOutcomePublicationException(String eventId, Throwable cause) {
        super("Failed to publish event outcome [eventId=" + eventId + "]", cause);
    }
}
