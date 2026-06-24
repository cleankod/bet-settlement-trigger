package eu.cleankod.settlementtrigger.application.port.out;

import eu.cleankod.settlementtrigger.domain.BetSettlement;

public interface BetSettlementPublisher {

    void publish(BetSettlement betSettlement);
}
