package com.f2pool.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<String>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("/".equals(uri)) {
            log.debug("404 root not found: {} {}", request.getMethod(), uri);
        } else {
            log.warn("404 not found: {} {}", request.getMethod(), uri);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new R<>(404, "Resource not found", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<String>> handleIllegalArgument(IllegalArgumentException e) {
        HttpStatus status = resolveStatusFromMessage(e.getMessage());
        log.warn("business error: status={}, msg={}", status.value(), e.getMessage());
        return ResponseEntity.status(status)
                .body(R.fail(status.value(), e.getMessage()));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            BindException.class
    })
    public ResponseEntity<R<String>> handleBadRequest(Exception e) {
        String msg = "Invalid request";
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            msg = e.getMessage();
        }
        log.warn("bad request: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(HttpStatus.BAD_REQUEST.value(), msg));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("method not allowed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "Method not allowed"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<String>> handleDataConflict(DataIntegrityViolationException e) {
        log.warn("data conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.fail(HttpStatus.CONFLICT.value(), "Data conflict"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<String>> handleException(Exception e) {
        log.error("Unhandled server exception: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "System error: " + e.getMessage()));
    }

    private HttpStatus resolveStatusFromMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return HttpStatus.BAD_REQUEST;
        }
        String text = msg.toLowerCase();
        if (text.contains("already exists") || text.contains("already audited")) {
            return HttpStatus.CONFLICT;
        }
        if (text.contains("password is incorrect")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (text.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (text.contains("does not belong") || text.contains("disabled") || text.contains("forbidden")) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
