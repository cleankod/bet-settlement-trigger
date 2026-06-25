package eu.cleankod.settlementtrigger.config;

public final class CorrelationId {

    /** MDC key used to carry the correlation ID throughout a request or message flow. */
    public static final String MDC_KEY = "correlationId";

    /** HTTP request/response header name for the correlation ID. */
    public static final String HTTP_HEADER = "X-Correlation-ID";

    /** Kafka message header name for propagating the correlation ID. */
    public static final String KAFKA_HEADER = "correlationId";

    private CorrelationId() {
    }
}
