package io.github.rezakaramad.probe;

import io.lettuce.core.RedisException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Translates exceptions into RFC 7807 {@link ProblemDetail} responses so API
 * clients (and the UI) always receive structured JSON instead of stack traces
 * or Spring's default error page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean-validation failures on request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed");
        pd.setTitle("Invalid request");
        pd.setType(URI.create("about:blank"));
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                pd.setProperty(fe.getField(), fe.getDefaultMessage()));
        return pd;
    }

    /** Validation on query params / path variables. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid request");
        return pd;
    }

    /** Any Valkey/Lettuce failure -> 503, since it's a downstream dependency. */
    @ExceptionHandler(RedisException.class)
    public ProblemDetail handleValkey(RedisException ex) {
        log.error("Valkey operation failed", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Valkey is unavailable: " + ex.getMessage());
        pd.setTitle("Valkey unavailable");
        return pd;
    }

    /** Catch-all so nothing leaks a stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error");
        pd.setTitle("Internal error");
        return pd;
    }
}
