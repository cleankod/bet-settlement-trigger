package eu.cleankod.settlementtrigger.application.service;

import eu.cleankod.settlementtrigger.application.port.in.SettleBetUseCase;
import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.BetSettlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Single writer for bet state. Settles a bet when a settlement command arrives.
 *
 * Idempotency: if the bet is already settled, the settlement is logged and skipped.
 * This handles duplicate delivery of the same settlement command.
 *
 * Reliability note: the save is not acknowledged back to the settlement adapter.
 * If the save succeeds but the message is not acknowledged, the command will be
 * redelivered and the idempotency check will prevent double settlement.
 */
@Service
public class BetSettlementService implements SettleBetUseCase {

    private static final Logger log = LoggerFactory.getLogger(BetSettlementService.class);

    private final BetRepository betRepository;

    public BetSettlementService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @Override
    public void settle(BetSettlement betSettlement) {
        betRepository.findById(betSettlement.betId()).ifPresentOrElse(
                bet -> {
                    if (bet.isPending()) {
                        Bet settledBet = bet.settle(betSettlement.eventWinnerId());
                        betRepository.save(settledBet);
                    } else {
                        log.info("Bet {} is already {} — skipping duplicate settlement", bet.id(), bet.status());
                    }
                },
                () -> log.warn("Bet {} not found — settlement command ignored", betSettlement.betId())
        );
    }
}
