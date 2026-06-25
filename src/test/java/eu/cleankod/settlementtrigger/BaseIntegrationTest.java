package eu.cleankod.settlementtrigger;

import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests.
 *
 * <p>Holds Testcontainers setup, Spring Boot test configuration, and shared autowired beans.
 * Individual test classes extend this class and contain only test scenarios.
 *
 * <p>Profile {@code local} activates {@code LocalBetSettlementPublisher},
 * enabling the full publish→settle flow without a real RocketMQ broker.
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = response.getHeaders().getLocation();
        String path = location.getPath();
        return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
    }
}
