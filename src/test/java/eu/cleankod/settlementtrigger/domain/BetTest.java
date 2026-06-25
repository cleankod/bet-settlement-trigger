package eu.cleankod.settlementtrigger.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BetTest {

    private static final long BET_ID = 1L;
    private static final String USER_ID = "user-alice";
    private static final String EVENT_ID = "event-final";
    private static final String MARKET_ID = "market-main";
    private static final String SELECTED_WINNER = "team-alpha";
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");

    private Bet pendingBet() {
        return new Bet(BET_ID, USER_ID, EVENT_ID, MARKET_ID, SELECTED_WINNER, AMOUNT, BetStatus.PENDING);
    }

    @Nested
    class WhenSettlingPendingBet {

        @Test
        void returnsWonWhenSelectedWinnerMatchesActualWinner() {
            // given
            Bet bet = pendingBet();

            // when
            Bet settled = bet.settle(SELECTED_WINNER);

            // then
            assertThat(settled.status()).isEqualTo(BetStatus.WON);
        }

        @Test
        void returnsLostWhenSelectedWinnerDiffersFromActualWinner() {
            // given
            Bet bet = pendingBet();

            // when
            Bet settled = bet.settle("team-beta");

            // then
            assertThat(settled.status()).isEqualTo(BetStatus.LOST);
        }

        @Test
        void preservesAllOtherFieldsAfterSettlement() {
            // given
            Bet bet = pendingBet();

            // when
            Bet settled = bet.settle(SELECTED_WINNER);

            // then
            assertThat(settled.id()).isEqualTo(BET_ID);
            assertThat(settled.userId()).isEqualTo(USER_ID);
            assertThat(settled.eventId()).isEqualTo(EVENT_ID);
            assertThat(settled.eventMarketId()).isEqualTo(MARKET_ID);
            assertThat(settled.selectedWinnerId()).isEqualTo(SELECTED_WINNER);
            assertThat(settled.betAmount()).isEqualByComparingTo(AMOUNT);
        }
    }

    @Nested
    class WhenSettlingAlreadySettledBet {

        @Test
        void throwsBetAlreadySettledExceptionForWonBet() {
            // given
            Bet wonBet = new Bet(BET_ID, USER_ID, EVENT_ID, MARKET_ID, SELECTED_WINNER, AMOUNT, BetStatus.WON);

            // when
            Throwable thrown = catchThrowable(() -> wonBet.settle(SELECTED_WINNER));

            // then
            assertThat(thrown).isInstanceOf(BetAlreadySettledException.class);
        }

        @Test
        void throwsBetAlreadySettledExceptionForLostBet() {
            // given
            Bet lostBet = new Bet(BET_ID, USER_ID, EVENT_ID, MARKET_ID, SELECTED_WINNER, AMOUNT, BetStatus.LOST);

            // when
            Throwable thrown = catchThrowable(() -> lostBet.settle("team-beta"));

            // then
            assertThat(thrown).isInstanceOf(BetAlreadySettledException.class);
        }
    }

    @Nested
    class WhenCheckingPendingStatus {

        @Test
        void returnsTrueForPendingBet() {
            assertThat(pendingBet().isPending()).isTrue();
        }

        @Test
        void returnsFalseForWonBet() {
            // given
            Bet wonBet = new Bet(BET_ID, USER_ID, EVENT_ID, MARKET_ID, SELECTED_WINNER, AMOUNT, BetStatus.WON);

            // when / then
            assertThat(wonBet.isPending()).isFalse();
        }

        @Test
        void returnsFalseForLostBet() {
            // given
            Bet lostBet = new Bet(BET_ID, USER_ID, EVENT_ID, MARKET_ID, SELECTED_WINNER, AMOUNT, BetStatus.LOST);

            // when / then
            assertThat(lostBet.isPending()).isFalse();
        }
    }
}
