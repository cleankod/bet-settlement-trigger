package eu.cleankod.settlementtrigger;

import eu.cleankod.settlementtrigger.domain.BetStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the event outcome flow.
 *
 * <p>Covers: REST validation, full WON/LOST settlement flows, idempotency on duplicate
 * event outcomes, and graceful handling when no matching bets exist.
 */
class EventOutcomeIntegrationTest extends BaseIntegrationTest {

    @Nested
    class WhenRestEndpointReceivesOutcome {

        @Test
        void returns202AcceptedForValidRequest() {
            // given
            Map<String, String> request = Map.of(
                    "eventId", "event-unknown",
                    "eventName", "Unknown Event",
                    "eventWinnerId", "team-x"
            );

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, Void.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        @Test
        void returns400WhenEventIdIsMissing() {
            // given
            Map<String, String> request = Map.of(
                    "eventName", "Some Event",
                    "eventWinnerId", "team-x"
            );

            // when
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400WhenEventWinnerIdIsMissing() {
            // given
            Map<String, String> request = Map.of(
                    "eventId", "event-unknown",
                    "eventName", "Some Event"
            );

            // when
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class WhenEventOutcomeFlowsEndToEnd {

        @Test
        void settlesBetAsWonWhenSelectedWinnerMatchesActualWinner() {
            // given — bet 100: event-won-scenario, selectedWinner=team-alpha
            Map<String, String> request = Map.of(
                    "eventId", "event-won-scenario",
                    "eventName", "League Final",
                    "eventWinnerId", "team-alpha"
            );

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, Void.class);

            // then — 202 accepted synchronously
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // and — bet eventually settled as WON via the async Kafka → local settle path
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(100L)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));
        }

        @Test
        void settlesBetAsLostWhenSelectedWinnerDiffersFromActualWinner() {
            // given — bet 101: event-lost-scenario, selectedWinner=team-alpha; winner will be team-beta
            Map<String, String> request = Map.of(
                    "eventId", "event-lost-scenario",
                    "eventName", "Cup Quarter-final",
                    "eventWinnerId", "team-beta"
            );

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, Void.class);

            // then — 202 accepted synchronously
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // and — bet eventually settled as LOST
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(101L)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.LOST));
        }

        @Test
        void doesNotResettleAlreadySettledBet() {
            // given — bet 102: event-idempotency-scenario, selectedWinner=team-gamma
            Map<String, String> request = Map.of(
                    "eventId", "event-idempotency-scenario",
                    "eventName", "Idempotency Test Event",
                    "eventWinnerId", "team-gamma"
            );

            // when — first settlement
            restTemplate.postForEntity("/api/v1/event-outcomes", request, Void.class);
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(102L)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));

            // when — duplicate event outcome sent
            restTemplate.postForEntity("/api/v1/event-outcomes", request, Void.class);

            // then — bet remains WON; duplicate is a no-op (findPendingByEventId returns empty)
            await().atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(102L)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));
        }

        @Test
        void doesNotFailWhenNoMatchingBetsExistForEvent() {
            // given — no bets seeded for this eventId
            Map<String, String> request = Map.of(
                    "eventId", "event-no-bets",
                    "eventName", "Event With No Bets",
                    "eventWinnerId", "team-z"
            );

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes", request, Void.class);

            // then — accepted without error; no settlement attempted
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }
    }
}
