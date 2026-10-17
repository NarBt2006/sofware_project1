package com.example.library.repository;

import com.example.library.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    /* ---- active (not yet returned) loans ---- */

    List<Loan> findByReturnDateIsNull();

    List<Loan> findByUserIdAndReturnDateIsNull(Long userId);

    /** True if the user already borrowed this book and has not returned it yet. */
    boolean existsByUserIdAndBookIdAndReturnDateIsNull(Long userId, Long bookId);

    /** True if any copy of this book is currently borrowed (blocks book deletion). */
    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    boolean existsByUserIdAndReturnDateIsNull(Long userId);

    /* ---- reports ---- */

    /** Open loans whose due date is already in the past = overdue today. */
    List<Loan> findByReturnDateIsNullAndDueDateBefore(LocalDate today);

    /* ---- history ---- */

    List<Loan> findByUserId(Long userId);

    long countByUserIdAndReturnDateIsNull(Long userId);

    /** Used when deleting a user: remove their returned-loan history with them. */
    @Modifying
    void deleteByUserId(Long userId);

    /** Used when deleting a book: remove its returned-loan history with it. */
    @Modifying
    void deleteByBookId(Long bookId);
}
