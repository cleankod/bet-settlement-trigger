package eu.cleankod.settlementtrigger.adapter.out.persistence;

import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.BetStatus;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("bets")
record BetEntity(
        @Id Long id,
        String userId,
        String eventId,
        String eventMarketId,
        String selectedWinnerId,
        BigDecimal betAmount,
        String status
) {

    static BetEntity from(UnsavedBet unsavedBet) {
        return new BetEntity(
                null,
                unsavedBet.userId(),
                unsavedBet.eventId(),
                unsavedBet.eventMarketId(),
                unsavedBet.selectedWinnerId(),
                unsavedBet.betAmount(),
                BetStatus.PENDING.name()
        );
    }

    static BetEntity from(Bet bet) {
        return new BetEntity(
                bet.id(),
                bet.userId(),
                bet.eventId(),
                bet.eventMarketId(),
                bet.selectedWinnerId(),
                bet.betAmount(),
                bet.status().name()
        );
    }

    Bet toDomain() {
        return new Bet(
                id,
                userId,
                eventId,
                eventMarketId,
                selectedWinnerId,
                betAmount,
                BetStatus.valueOf(status)
        );
    }
}
