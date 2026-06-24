package eu.cleankod.settlementtrigger.domain;

import java.math.BigDecimal;

/**
 * A bet that has not yet been persisted. Has no database id.
 * Once saved, the persistence adapter returns a {@link Bet} with a guaranteed id.
 */
public record UnsavedBet(
        String userId,
        String eventId,
        String eventMarketId,
        String selectedWinnerId,
        BigDecimal betAmount
) {
}
