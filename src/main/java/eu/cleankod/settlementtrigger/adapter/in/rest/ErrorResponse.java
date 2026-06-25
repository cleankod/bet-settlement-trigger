package eu.cleankod.settlementtrigger.adapter.in.rest;

import java.util.List;

record ErrorResponse(
        String errorId,
        String code,
        String message,
        List<String> details
) {

    static ErrorResponse of(String errorId, String code, String message) {
        return new ErrorResponse(errorId, code, message, List.of());
    }

    static ErrorResponse of(String errorId, String code, String message, List<String> details) {
        return new ErrorResponse(errorId, code, message, details);
    }
}
