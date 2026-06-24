package eu.cleankod.settlementtrigger.domain;

import java.math.BigDecimal;

/**
 * A persisted bet with a guaranteed database id.
 * Use {@link UnsavedBet} to represent a bet before it has been saved.
 */
public record Bet(
        long id,
        String userId,
        String eventId,
        String eventMarketId,
        String selectedWinnerId,
        BigDecimal betAmount,
        BetStatus status
) {

    public Bet settle(String actualWinnerId) {
        if (!isPending()) {
            throw new BetAlreadySettledException(id, status);
        }
        BetStatus result = selectedWinnerId.equals(actualWinnerId) ? BetStatus.WON : BetStatus.LOST;
        return new Bet(id, userId, eventId, eventMarketId, selectedWinnerId, betAmount, result);
    }

    public boolean isPending() {
        return status == BetStatus.PENDING;
    }
}
