package eu.cleankod.settlementtrigger.application.port.in;

import eu.cleankod.settlementtrigger.domain.EventOutcome;

public interface PublishEventOutcomeUseCase {

    void publish(EventOutcome eventOutcome);
}
