package eu.cleankod.settlementtrigger.domain;

import java.math.BigDecimal;

/**
 * Represents a settlement command for a single bet.
 *
 * Dual-purpose: published on the settlement topic for downstream consumers
 * and consumed by the settlement adapter to drive the state transition.
 *
 * The WON/LOST outcome is intentionally absent. Consumers who need it can
 * derive it by comparing {@code selectedWinnerId} to {@code eventWinnerId}.
 * The authoritative derivation lives in {@link Bet#settle(String)}.
 */
public record BetSettlement(
        long betId,
        String userId,
        String eventId,
        String eventWinnerId,
        String selectedWinnerId,
        BigDecimal betAmount
) {

    public static BetSettlement of(Bet pendingBet, EventOutcome eventOutcome) {
        if (!pendingBet.eventId().equals(eventOutcome.eventId())) {
            throw new EventMismatchException(pendingBet.id(), pendingBet.eventId(), eventOutcome.eventId());
        }
        return new BetSettlement(
                pendingBet.id(),
                pendingBet.userId(),
                pendingBet.eventId(),
                eventOutcome.eventWinnerId(),
                pendingBet.selectedWinnerId(),
                pendingBet.betAmount()
        );
    }
}
