package eu.cleankod.settlementtrigger;

import eu.cleankod.settlementtrigger.domain.BetStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.net.URI;
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

    // Settlement matching is by eventId only — the market field is carried for audit
    // but does not affect which bets are matched or how they are settled.
    private static final String ANY_MARKET = "market-main";

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
    class WhenBetIsPlaced {

        @Test
        void returns201CreatedWithLocationHeader() {
            // given
            Map<String, Object> request = Map.of(
                    "userId", "user-alice",
                    "eventId", "event-place-bet-test",
                    "eventMarketId", ANY_MARKET,
                    "selectedWinnerId", "team-alpha",
                    "betAmount", BigDecimal.valueOf(50.00)
            );

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/bets", request, Void.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation()).isNotNull()
                    .extracting(URI::getPath)
                    .asString()
                    .startsWith("/api/v1/bets/");
        }

        @Test
        void returns400WhenBetAmountIsZero() {
            // given
            Map<String, Object> request = Map.of(
                    "userId", "user-alice",
                    "eventId", "event-place-bet-test",
                    "eventMarketId", ANY_MARKET,
                    "selectedWinnerId", "team-alpha",
                    "betAmount", BigDecimal.ZERO
            );

            // when
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/bets", request, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class WhenEventOutcomeFlowsEndToEnd {

        @Test
        void settlesBetAsWonWhenSelectedWinnerMatchesActualWinner() {
            // given — place a bet on team-alpha for this event
            long betId = placeBet("user-alice", "event-won-scenario", ANY_MARKET, "team-alpha", BigDecimal.valueOf(50.00));

            // when — event outcome arrives: team-alpha won
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes",
                    Map.of("eventId", "event-won-scenario", "eventName", "League Final", "eventWinnerId", "team-alpha"),
                    Void.class);

            // then — 202 accepted synchronously
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // and — bet eventually settled as WON via the async Kafka → local settle path
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(betId)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));
        }

        @Test
        void settlesBetAsLostWhenSelectedWinnerDiffersFromActualWinner() {
            // given — place a bet on team-alpha, but actual winner will be team-beta
            long betId = placeBet("user-bob", "event-lost-scenario", ANY_MARKET, "team-alpha", BigDecimal.valueOf(30.00));

            // when — event outcome arrives: team-beta won
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes",
                    Map.of("eventId", "event-lost-scenario", "eventName", "Cup Quarter-final", "eventWinnerId", "team-beta"),
                    Void.class);

            // then — 202 accepted synchronously
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

            // and — bet eventually settled as LOST
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(betId)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.LOST));
        }

        @Test
        void doesNotResettleAlreadySettledBet() {
            // given — place a bet on team-gamma
            long betId = placeBet("user-carol", "event-idempotency-scenario", ANY_MARKET, "team-gamma", BigDecimal.valueOf(20.00));

            Map<String, String> request = Map.of(
                    "eventId", "event-idempotency-scenario",
                    "eventName", "Idempotency Test Event",
                    "eventWinnerId", "team-gamma"
            );

            // when — first settlement
            restTemplate.postForEntity("/api/v1/event-outcomes", request, Void.class);
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(betId)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));

            // when — duplicate event outcome sent
            restTemplate.postForEntity("/api/v1/event-outcomes", request, Void.class);

            // then — bet remains WON throughout the window; proves duplicate is a no-op
            await().during(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            betRepository.findById(betId)
                                    .map(bet -> bet.status())
                                    .orElseThrow())
                            .isEqualTo(BetStatus.WON));
        }

        @Test
        void doesNotFailWhenNoMatchingBetsExistForEvent() {
            // given — no bets placed for this eventId

            // when
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/event-outcomes",
                    Map.of("eventId", "event-no-bets", "eventName", "Event With No Bets", "eventWinnerId", "team-z"),
                    Void.class);

            // then — accepted without error; no settlement attempted
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }
    }
}
