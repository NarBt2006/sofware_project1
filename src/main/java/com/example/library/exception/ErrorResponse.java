package com.example.library.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform error body returned by all exceptions. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) { // populated only for bean-validation failures
}
