package com.example.library.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate joinedAt,
        long activeLoans) {
}
