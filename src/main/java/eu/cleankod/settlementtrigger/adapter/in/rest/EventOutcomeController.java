package eu.cleankod.settlementtrigger.adapter.in.rest;

import eu.cleankod.settlementtrigger.application.port.in.PublishEventOutcomeUseCase;
import eu.cleankod.settlementtrigger.domain.EventOutcome;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class EventOutcomeController {

    private final PublishEventOutcomeUseCase publishEventOutcomeUseCase;

    EventOutcomeController(PublishEventOutcomeUseCase publishEventOutcomeUseCase) {
        this.publishEventOutcomeUseCase = publishEventOutcomeUseCase;
    }

    @PostMapping("/event-outcomes")
    ResponseEntity<Void> receiveEventOutcome(@Valid @RequestBody EventOutcomeRequest request) {
        publishEventOutcomeUseCase.publish(new EventOutcome(request.eventId(), request.eventName(), request.eventWinnerId()));
        return ResponseEntity.accepted().build();
    }
}
