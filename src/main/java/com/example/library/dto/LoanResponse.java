package com.example.library.dto;

import java.time.LocalDate;

/**
 * Loan view returned by the API.
 *
 * fineAmount:
 *  - for a RETURNED loan: the final fine stored at return time
 *  - for an ACTIVE loan: the fine accrued so far, computed live against today
 */
public record LoanResponse(
        Long id,
        Long userId,
        String userName,
        Long bookId,
        String bookTitle,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        String status,      // ACTIVE or RETURNED
        boolean overdue,    // true when past due (active loans) or was returned late
        long overdueDays,
        long fineAmount) {  // in Toman
}
