package eu.cleankod.settlementtrigger;

import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

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
 * <p>H2 is seeded with sample bets by {@code V2__seed_sample_bets.sql}
 * (src/test/resources/db/migration/). Each test scenario uses a distinct
 * {@code event_id} to avoid inter-test interference.
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
}
