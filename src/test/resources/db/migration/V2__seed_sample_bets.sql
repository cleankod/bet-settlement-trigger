-- Sample bets used by integration tests.
-- Each scenario uses a distinct event_id so tests do not interfere with each other.

INSERT INTO bets (id, user_id, event_id, event_market_id, selected_winner_id, bet_amount, status)
VALUES
    -- event-won-scenario: bet 100 selected team-alpha; actual winner will be team-alpha → WON
    (100, 'user-alice',  'event-won-scenario',         'market-main', 'team-alpha', 50.00, 'PENDING'),

    -- event-lost-scenario: bet 101 selected team-alpha; actual winner will be team-beta → LOST
    (101, 'user-bob',    'event-lost-scenario',        'market-main', 'team-alpha', 30.00, 'PENDING'),

    -- event-idempotency-scenario: bet 102 for testing that duplicate settlement is a no-op
    (102, 'user-carol',  'event-idempotency-scenario', 'market-main', 'team-gamma', 20.00, 'PENDING');
