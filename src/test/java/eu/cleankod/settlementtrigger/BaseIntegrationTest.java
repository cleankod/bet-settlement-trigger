package eu.cleankod.settlementtrigger;

import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

/**
 * Base class for integration tests.
 *
 * <p>Holds Testcontainers setup, Spring Boot test configuration, and shared autowired beans.
 * Individual test classes extend this class and contain only test scenarios.
 *
 * <p>Profile {@code local} activates
 * {@link eu.cleankod.settlementtrigger.adapter.out.settlement.LocalBetSettlementPublisher},
 * enabling the full publish→settle flow without a real RocketMQ broker.
 *
 * <p>Test bets are created programmatically via {@link #placeBet} using the REST endpoint,
 * so no SQL seed migration is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BetSettlementTriggerApplication.class)
@AutoConfigureTestRestTemplate
@Testcontainers
@ActiveProfiles("local")
abstract class BaseIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:4.0.0")
                    .asCompatibleSubstituteFor("apache/kafka"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected BetRepository betRepository;

    /**
     * Places a bet via the REST API and returns the assigned bet ID extracted from the Location header.
     */
    protected long placeBet(String userId, String eventId, String eventMarketId,
                             String selectedWinnerId, BigDecimal betAmount) {
        Map<String, Object> request = Map.of(
                "userId", userId,
                "eventId", eventId,
                "eventMarketId", eventMarketId,
                "selectedWinnerId", selectedWinnerId,
                "betAmount", betAmount
        );
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/bets", request, Void.class);
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("POST /api/v1/bets did not return a Location header");
        }
        String path = location.getPath();
        return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
    }
}
