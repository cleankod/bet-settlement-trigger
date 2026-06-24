package eu.cleankod.settlementtrigger.application.port.out;

import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;

import java.util.List;
import java.util.Optional;

public interface BetRepository {

    /**
     * Persists a new bet and returns it with its assigned database id.
     */
    Bet save(UnsavedBet unsavedBet);

    /**
     * Updates an existing bet's state.
     */
    void save(Bet bet);

    Optional<Bet> findById(long id);

    List<Bet> findPendingByEventId(String eventId);
}
