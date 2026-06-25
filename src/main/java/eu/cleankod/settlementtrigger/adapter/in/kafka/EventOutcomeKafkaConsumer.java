package eu.cleankod.settlementtrigger.adapter.in.kafka;

import eu.cleankod.settlementtrigger.application.port.in.HandleEventOutcomeUseCase;
import eu.cleankod.settlementtrigger.config.CorrelationId;
import eu.cleankod.settlementtrigger.config.KafkaTopics;
import eu.cleankod.settlementtrigger.domain.EventOutcome;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Inbound Kafka adapter: consumes event outcomes from the {@value KafkaTopics#EVENT_OUTCOMES} topic
 * and delegates to {@link HandleEventOutcomeUseCase}.
 *
 * <p>Delivery guarantee: at-least-once. The use case is idempotent — already-settled bets
 * are silently ignored. A poison message (undeserializable payload or a permanently-failing
 * handler) will be redelivered indefinitely by the default {@code DefaultErrorHandler}.
 * A {@code DeadLetterPublishingRecoverer} with a fixed retry limit is a future improvement.
 */
@Component
class EventOutcomeKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeKafkaConsumer.class);

    private final HandleEventOutcomeUseCase handleEventOutcomeUseCase;

    EventOutcomeKafkaConsumer(HandleEventOutcomeUseCase handleEventOutcomeUseCase) {
        this.handleEventOutcomeUseCase = handleEventOutcomeUseCase;
    }

    @KafkaListener(topics = KafkaTopics.EVENT_OUTCOMES)
    void consume(ConsumerRecord<String, EventOutcome> consumerRecord) {
        String correlationId = extractCorrelationId(consumerRecord);
        try {
            MDC.put(CorrelationId.MDC_KEY, correlationId);
            EventOutcome eventOutcome = consumerRecord.value();
            log.info("Received event outcome from Kafka [eventId={}, eventName={}]",
                    eventOutcome.eventId(), eventOutcome.eventName());
            handleEventOutcomeUseCase.handle(eventOutcome);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private String extractCorrelationId(ConsumerRecord<String, EventOutcome> consumerRecord) {
        Header header = consumerRecord.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return UUID.randomUUID().toString();
    }
}
