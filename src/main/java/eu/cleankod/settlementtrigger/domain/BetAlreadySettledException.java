package eu.cleankod.settlementtrigger.domain;

public class BetAlreadySettledException extends RuntimeException {

    public BetAlreadySettledException(long betId, BetStatus currentStatus) {
        super("Bet " + betId + " cannot be settled because it is already " + currentStatus);
    }
}
