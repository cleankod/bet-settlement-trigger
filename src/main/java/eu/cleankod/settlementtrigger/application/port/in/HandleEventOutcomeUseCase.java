package eu.cleankod.settlementtrigger.application.port.in;

import eu.cleankod.settlementtrigger.domain.EventOutcome;

public interface HandleEventOutcomeUseCase {

    void handle(EventOutcome eventOutcome);
}
