package eu.cleankod.settlementtrigger.application.port.out;

import eu.cleankod.settlementtrigger.domain.EventOutcome;

public interface EventOutcomePublisher {

    void publish(EventOutcome eventOutcome);
}
