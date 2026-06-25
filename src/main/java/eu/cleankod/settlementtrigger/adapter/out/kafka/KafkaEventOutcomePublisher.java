package eu.cleankod.settlementtrigger.adapter.out.kafka;

import eu.cleankod.settlementtrigger.application.port.out.EventOutcomePublicationException;
import eu.cleankod.settlementtrigger.application.port.out.EventOutcomePublisher;
import eu.cleankod.settlementtrigger.config.CorrelationId;
import eu.cleankod.settlementtrigger.config.KafkaTopics;
import eu.cleankod.settlementtrigger.domain.EventOutcome;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
class KafkaEventOutcomePublisher implements EventOutcomePublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventOutcomePublisher.class);

    private final KafkaTemplate<String, EventOutcome> kafkaTemplate;
    private final Duration publishTimeout;

    KafkaEventOutcomePublisher(
            KafkaTemplate<String, EventOutcome> kafkaTemplate,
            @Value("${app.kafka.publish-timeout:5s}") Duration publishTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishTimeout = publishTimeout;
    }

    @Override
    public void publish(EventOutcome eventOutcome) {
        ProducerRecord<String, EventOutcome> producerRecord =
                new ProducerRecord<>(KafkaTopics.EVENT_OUTCOMES, eventOutcome.eventId(), eventOutcome);
        // CorrelationIdFilter always populates MDC before the controller runs for HTTP requests
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        producerRecord.headers().add(
                new RecordHeader(CorrelationId.KAFKA_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));
        try {
            kafkaTemplate.send(producerRecord).get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.info("Published event outcome to Kafka [eventId={}, eventName={}]",
                    eventOutcome.eventId(), eventOutcome.eventName());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new EventOutcomePublicationException(eventOutcome.eventId(), interruptedException);
        } catch (ExecutionException | TimeoutException exception) {
            throw new EventOutcomePublicationException(eventOutcome.eventId(), exception);
        }
    }
}
