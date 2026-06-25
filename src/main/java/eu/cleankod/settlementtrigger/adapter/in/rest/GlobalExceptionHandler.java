package eu.cleankod.settlementtrigger.adapter.in.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception) {
        String errorId = ErrorIdGenerator.newErrorId();
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        log.warn("Validation failure [errorId={}]: {}", errorId, details);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(errorId, "VALIDATION_FAILURE", "Request validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        String errorId = ErrorIdGenerator.newErrorId();
        log.error("Unexpected error [errorId={}]", errorId, exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(errorId, "INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
