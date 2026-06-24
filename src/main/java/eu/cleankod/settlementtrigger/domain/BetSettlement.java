package eu.cleankod.settlementtrigger.domain;

import java.math.BigDecimal;

public record BetSettlement(
        long betId,
        String userId,
        String eventId,
        String eventWinnerId,
        String selectedWinnerId,
        BigDecimal betAmount,
        BetStatus outcome
) {

    public static BetSettlement of(Bet pendingBet, EventOutcome eventOutcome) {
        if (!pendingBet.eventId().equals(eventOutcome.eventId())) {
            throw new EventMismatchException(pendingBet.id(), pendingBet.eventId(), eventOutcome.eventId());
        }
        BetStatus settlementOutcome = pendingBet.selectedWinnerId().equals(eventOutcome.eventWinnerId())
                ? BetStatus.WON
                : BetStatus.LOST;
        return new BetSettlement(
                pendingBet.id(),
                pendingBet.userId(),
                pendingBet.eventId(),
                eventOutcome.eventWinnerId(),
                pendingBet.selectedWinnerId(),
                pendingBet.betAmount(),
                settlementOutcome
        );
    }
}
