package eu.cleankod.settlementtrigger.application.service;

import eu.cleankod.settlementtrigger.application.port.in.PublishEventOutcomeUseCase;
import eu.cleankod.settlementtrigger.application.port.out.EventOutcomePublisher;
import eu.cleankod.settlementtrigger.domain.EventOutcome;
import org.springframework.stereotype.Service;

@Service
public class EventOutcomePublishingService implements PublishEventOutcomeUseCase {

    private final EventOutcomePublisher eventOutcomePublisher;

    public EventOutcomePublishingService(EventOutcomePublisher eventOutcomePublisher) {
        this.eventOutcomePublisher = eventOutcomePublisher;
    }

    @Override
    public void publish(EventOutcome eventOutcome) {
        eventOutcomePublisher.publish(eventOutcome);
    }
}
