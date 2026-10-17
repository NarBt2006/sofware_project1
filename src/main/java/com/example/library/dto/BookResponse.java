package com.example.library.dto;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        int totalCopies,
        int availableCopies,
        boolean available) {
}
