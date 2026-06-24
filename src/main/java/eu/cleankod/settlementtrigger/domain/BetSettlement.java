package eu.cleankod.settlementtrigger.domain;

import java.math.BigDecimal;

public record BetSettlement(
        long betId,
        String userId,
        String eventId,
        String eventWinnerId,
        String selectedWinnerId,
        BigDecimal betAmount,
        BetStatus status
) {

    public static BetSettlement of(Bet settledBet, EventOutcome outcome) {
        if (!settledBet.eventId().equals(outcome.eventId())) {
            throw new EventMismatchException(settledBet.id(), settledBet.eventId(), outcome.eventId());
        }
        return new BetSettlement(
                settledBet.id(),
                settledBet.userId(),
                settledBet.eventId(),
                outcome.eventWinnerId(),
                settledBet.selectedWinnerId(),
                settledBet.betAmount(),
                settledBet.status()
        );
    }
}
