package com.example.library.dto;

import jakarta.validation.constraints.NotNull;

/** Request body for borrowing a book: POST /api/loans. */
public record BorrowRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "bookId is required")
        Long bookId) {
}
