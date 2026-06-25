package eu.cleankod.settlementtrigger.application.service;

import eu.cleankod.settlementtrigger.application.port.in.PlaceBetUseCase;
import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;
import org.springframework.stereotype.Service;

@Service
public class BetPlacementService implements PlaceBetUseCase {

    private final BetRepository betRepository;

    public BetPlacementService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @Override
    public Bet place(UnsavedBet unsavedBet) {
        return betRepository.save(unsavedBet);
    }
}
