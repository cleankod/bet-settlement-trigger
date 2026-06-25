package eu.cleankod.settlementtrigger.adapter.out.settlement;

import eu.cleankod.settlementtrigger.application.port.out.BetSettlementPublisher;
import eu.cleankod.settlementtrigger.domain.BetSettlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock outbound settlement adapter: logs the settlement command to simulate
 * publishing to a RocketMQ topic ({@code bet-settlements}).
 *
 * <p>Active on all profiles except {@code local}. With no active profile (default)
 * this is the only {@link BetSettlementPublisher} bean — settlements are logged but
 * never consumed/settled. This reflects the expected posture when no real RocketMQ
 * consumer is present.
 *
 * <p>On the {@code local} profile, {@link LocalBetSettlementPublisher} is used
 * instead (mutual exclusivity via {@code @Profile("!local")} / {@code @Profile("local")}).
 *
 * <p>Real RocketMQ integration is a future improvement.
 */
@Component
@Profile("!local")
class LoggingBetSettlementPublisher implements BetSettlementPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingBetSettlementPublisher.class);

    @Override
    public void publish(BetSettlement betSettlement) {
        log.info("[RocketMQ-mock] bet-settlements << betId={} userId={} eventId={} eventWinnerId={} selectedWinnerId={} betAmount={}",
                betSettlement.betId(),
                betSettlement.userId(),
                betSettlement.eventId(),
                betSettlement.eventWinnerId(),
                betSettlement.selectedWinnerId(),
                betSettlement.betAmount());
    }
}
