package com.org.learning.payment.web;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.org.learning.payment.exception.PaymentNotFoundException;
import com.org.learning.payment.web.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidTypeIdException;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class PaymentControllerAdvice {

    /** Allowed discriminator values, derived from the @JsonSubTypes allow-list — single source of truth. */
    private static final String ALLOWED_TYPES = Arrays.stream(
                    PaymentRequest.class.getAnnotation(JsonSubTypes.class).value())
            .map(JsonSubTypes.Type::name)
            .collect(Collectors.joining(", "));

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Unknown/missing {@code type} discriminator — Jackson throws {@link InvalidTypeIdException},
     * which Spring's message converter wraps in {@link HttpMessageNotReadableException}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        if (findInvalidTypeId(ex) instanceof InvalidTypeIdException typeEx) {
            var detail = "Unknown payment type '%s'. Allowed types: %s"
                    .formatted(typeEx.getTypeId(), ALLOWED_TYPES);
            log.warn(detail);
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        }
        log.warn("Malformed request body: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    /** Jackson 3 exceptions are unchecked — cover the case where one escapes unwrapped. */
    @ExceptionHandler(InvalidTypeIdException.class)
    public ProblemDetail handleInvalidTypeId(InvalidTypeIdException ex) {
        var detail = "Unknown payment type '%s'. Allowed types: %s"
                .formatted(ex.getTypeId(), ALLOWED_TYPES);
        log.warn(detail);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleNotFound(PaymentNotFoundException ex) {
        log.warn(ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private Throwable findInvalidTypeId(Throwable ex) {
        for (var cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidTypeIdException) {
                return cause;
            }
        }
        return null;
    }
}
