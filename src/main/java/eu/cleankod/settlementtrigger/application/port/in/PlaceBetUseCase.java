package eu.cleankod.settlementtrigger.application.port.in;

import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;

public interface PlaceBetUseCase {

    Bet place(UnsavedBet unsavedBet);
}
