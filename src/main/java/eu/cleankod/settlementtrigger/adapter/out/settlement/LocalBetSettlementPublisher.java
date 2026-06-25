package eu.cleankod.settlementtrigger.adapter.out.settlement;

import eu.cleankod.settlementtrigger.application.port.in.SettleBetUseCase;
import eu.cleankod.settlementtrigger.application.port.out.BetSettlementPublisher;
import eu.cleankod.settlementtrigger.domain.BetSettlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local settlement publisher: logs the settlement command and immediately forwards it
 * to {@link SettleBetUseCase} so the full publish→settle flow is exercisable without
 * a real RocketMQ broker.
 *
 * <p>Active only on the {@code local} profile (mutually exclusive with
 * {@link LoggingBetSettlementPublisher} which guards itself with {@code @Profile("!local")}).
 * Forwards directly through the {@link SettleBetUseCase} inbound port, keeping the
 * dependency rule intact — no coupling to adapter classes.
 *
 * <p>In production, a {@code @RocketMQMessageListener} inbound adapter would deliver
 * messages to the same use case independently.
 */
@Component
@Profile("local")
class LocalBetSettlementPublisher implements BetSettlementPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalBetSettlementPublisher.class);

    private final SettleBetUseCase settleBetUseCase;

    LocalBetSettlementPublisher(SettleBetUseCase settleBetUseCase) {
        this.settleBetUseCase = settleBetUseCase;
    }

    @Override
    public void publish(BetSettlement betSettlement) {
        log.info("[RocketMQ-mock] bet-settlements << betId={} userId={} eventId={} eventWinnerId={} selectedWinnerId={} betAmount={}",
                betSettlement.betId(),
                betSettlement.userId(),
                betSettlement.eventId(),
                betSettlement.eventWinnerId(),
                betSettlement.selectedWinnerId(),
                betSettlement.betAmount());
        log.debug("LocalBetSettlementPublisher forwarding settlement command [betId={}]", betSettlement.betId());
        settleBetUseCase.settle(betSettlement);
    }
}
