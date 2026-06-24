package eu.cleankod.settlementtrigger.application.service;

import eu.cleankod.settlementtrigger.application.port.in.HandleEventOutcomeUseCase;
import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import eu.cleankod.settlementtrigger.application.port.out.BetSettlementPublisher;
import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.BetSettlement;
import eu.cleankod.settlementtrigger.domain.EventOutcome;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Matches a Kafka event outcome to pending bets and publishes a settlement command for each.
 * Does NOT modify bet state — state transition is owned by {@link BetSettlementService}.
 *
 * Idempotency: findPendingByEventId only returns bets that have not yet been settled,
 * so re-delivery of the same event outcome is a no-op for already-settled bets.
 *
 * Reliability note: publishing is not atomic with the read. If publishing fails for a bet,
 * the bet remains pending and will be retried on the next delivery of the same event outcome.
 * This is safe under at-least-once delivery.
 *
 * Scaling note: all pending bets for an event are loaded and published in a single call.
 * For very popular events this may be a large batch — acceptable for assignment scope.
 */
@Service
public class EventOutcomeHandlingService implements HandleEventOutcomeUseCase {

    private final BetRepository betRepository;
    private final BetSettlementPublisher betSettlementPublisher;

    public EventOutcomeHandlingService(BetRepository betRepository, BetSettlementPublisher betSettlementPublisher) {
        this.betRepository = betRepository;
        this.betSettlementPublisher = betSettlementPublisher;
    }

    @Override
    public void handle(EventOutcome eventOutcome) {
        List<Bet> pendingBets = betRepository.findPendingByEventId(eventOutcome.eventId());
        for (Bet pendingBet : pendingBets) {
            betSettlementPublisher.publish(BetSettlement.of(pendingBet, eventOutcome));
        }
    }
}
