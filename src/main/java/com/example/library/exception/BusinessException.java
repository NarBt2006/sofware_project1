package com.example.library.exception;

/** A business rule was violated (no copies left, duplicate borrow, ...) -> HTTP 409. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
