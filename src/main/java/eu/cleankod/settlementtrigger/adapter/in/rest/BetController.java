package eu.cleankod.settlementtrigger.adapter.in.rest;

import eu.cleankod.settlementtrigger.application.port.in.PlaceBetUseCase;
import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
class BetController {

    private final PlaceBetUseCase placeBetUseCase;

    BetController(PlaceBetUseCase placeBetUseCase) {
        this.placeBetUseCase = placeBetUseCase;
    }

    @PostMapping("/bets")
    ResponseEntity<Void> placeBet(@Valid @RequestBody PlaceBetRequest request,
                                  UriComponentsBuilder uriComponentsBuilder) {
        Bet bet = placeBetUseCase.place(new UnsavedBet(
                request.userId(),
                request.eventId(),
                request.eventMarketId(),
                request.selectedWinnerId(),
                request.betAmount()
        ));
        URI location = uriComponentsBuilder.path("/api/v1/bets/{id}").buildAndExpand(bet.id()).toUri();
        return ResponseEntity.created(location).build();
    }
}
