package eu.cleankod.settlementtrigger.adapter.in.rest;

import java.security.SecureRandom;

final class ErrorIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ErrorIdGenerator() {
    }

    static String newErrorId() {
        long value = RANDOM.nextLong() & Long.MAX_VALUE;
        return Long.toString(value, 36);
    }
}
