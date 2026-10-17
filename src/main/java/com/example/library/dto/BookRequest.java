package com.example.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a book.
 * ISBN is optional; availableCopies starts equal to totalCopies.
 */
public record BookRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "author is required")
        String author,

        String isbn,

        @NotNull(message = "totalCopies is required")
        @Min(value = 1, message = "totalCopies must be at least 1")
        Integer totalCopies) {
}
