package eu.cleankod.settlementtrigger.application.port.in;

import eu.cleankod.settlementtrigger.domain.BetSettlement;

public interface SettleBetUseCase {

    void settle(BetSettlement betSettlement);
}
