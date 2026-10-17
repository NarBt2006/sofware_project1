package com.example.library.dto;

import java.util.List;

/**
 * Fine summary for one user (GET /api/users/{id}/fines):
 * settled = final fines of already returned loans,
 * accruing = fines accrued so far on still-open loans (recomputed daily).
 */
public record UserFineResponse(
        Long userId,
        String userName,
        long settledFine,
        long accruingFine,
        long totalFine,
        List<LoanResponse> loans) {
}
