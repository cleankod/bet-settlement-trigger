package eu.cleankod.settlementtrigger.domain;

public class EventMismatchException extends RuntimeException {

    public EventMismatchException(long betId, String betEventId, String outcomeEventId) {
        super("Cannot settle bet " + betId + ": bet event " + betEventId
                + " does not match outcome event " + outcomeEventId);
    }
}
