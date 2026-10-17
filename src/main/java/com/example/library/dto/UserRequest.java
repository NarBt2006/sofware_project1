package com.example.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Request body for creating a user.
 * joinedAt is optional and defaults to today.
 */
public record UserRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        String phone,

        LocalDate joinedAt) {
}
