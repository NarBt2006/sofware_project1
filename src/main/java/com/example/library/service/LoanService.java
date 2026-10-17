package com.example.library.service;

import com.example.library.config.LibraryProperties;
import com.example.library.dto.BorrowRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.dto.UserFineResponse;
import com.example.library.entity.Book;
import com.example.library.entity.Loan;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.exception.NotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Core borrow/return workflow and fine calculation.
 *
 * Fine rule: dueDate = borrowDate + loan period (default 14 days).
 * Every day AFTER the due date costs the fixed daily fine:
 *   overdueDays = ChronoUnit.DAYS.between(dueDate, referenceDate)  (only if positive)
 *   fine        = overdueDays * daily fine amount
 * referenceDate is today for open loans (accruing fine, recomputed on every read)
 * and the actual returnDate for returned loans (final fine, stored once).
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LibraryProperties properties;

    public LoanService(LoanRepository loanRepository,
                       UserRepository userRepository,
                       BookRepository bookRepository,
                       LibraryProperties properties) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.properties = properties;
    }

    @Transactional
    public LoanResponse borrow(BorrowRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User with id " + request.userId() + " not found"));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new NotFoundException("Book with id " + request.bookId() + " not found"));

        if (loanRepository.existsByUserIdAndBookIdAndReturnDateIsNull(user.getId(), book.getId())) {
            throw new BusinessException("User '" + user.getName()
                    + "' already borrowed '" + book.getTitle() + "' and has not returned it yet");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No copies of '" + book.getTitle() + "' are currently available");
        }

        LocalDate today = LocalDate.now();
        Loan loan = new Loan(user, book, today, today.plusDays(properties.loanPeriodDays()));
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        return toResponse(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan with id " + loanId + " not found"));
        if (loan.getReturnDate() != null) {
            throw new BusinessException(
                    "Loan " + loanId + " was already returned on " + loan.getReturnDate());
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);
        // Final fine is fixed at return time based on the actual return date.
        loan.setFineAmount(calculateFine(loan.getDueDate(), today));
        loan.getBook().setAvailableCopies(loan.getBook().getAvailableCopies() + 1);
        return toResponse(loan);
    }

    /** All loans, optionally only the active (not yet returned) ones. */
    @Transactional(readOnly = true)
    public List<LoanResponse> findAll(Boolean activeOnly) {
        List<Loan> loans = Boolean.TRUE.equals(activeOnly)
                ? loanRepository.findByReturnDateIsNull()
                : loanRepository.findAll();
        return loans.stream()
                .sorted(Comparator.comparing(Loan::getId))
                .map(this::toResponse)
                .toList();
    }

    /** Loan history of one user, optionally only the active ones. */
    @Transactional(readOnly = true)
    public List<LoanResponse> findByUser(Long userId, Boolean activeOnly) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id " + userId + " not found");
        }
        List<Loan> loans = Boolean.TRUE.equals(activeOnly)
                ? loanRepository.findByUserIdAndReturnDateIsNull(userId)
                : loanRepository.findByUserId(userId);
        return loans.stream()
                .sorted(Comparator.comparing(Loan::getId))
                .map(this::toResponse)
                .toList();
    }

    /** Open loans past their due date, each with the fine accrued up to today. */
    @Transactional(readOnly = true)
    public List<LoanResponse> findOverdue() {
        return loanRepository.findByReturnDateIsNullAndDueDateBefore(LocalDate.now()).stream()
                .sorted(Comparator.comparing(Loan::getDueDate))
                .map(this::toResponse)
                .toList();
    }

    /** Total fine per user: settled (stored) fines + fines still accruing on open loans. */
    @Transactional(readOnly = true)
    public UserFineResponse userFines(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        List<LoanResponse> loans = loanRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Loan::getId))
                .map(this::toResponse)
                .toList();

        long settled = loans.stream()
                .filter(l -> "RETURNED".equals(l.status()))
                .mapToLong(LoanResponse::fineAmount)
                .sum();
        long accruing = loans.stream()
                .filter(l -> "ACTIVE".equals(l.status()))
                .mapToLong(LoanResponse::fineAmount)
                .sum();

        return new UserFineResponse(userId, user.getName(), settled, accruing,
                settled + accruing, loans);
    }

    /** Fine = overdue days × daily amount; days count only AFTER the due date. */
    private long calculateFine(LocalDate dueDate, LocalDate referenceDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, referenceDate);
        return overdueDays > 0 ? overdueDays * properties.fine().dailyAmount() : 0L;
    }

    private LoanResponse toResponse(Loan loan) {
        boolean returned = loan.getReturnDate() != null;
        // Open loans are judged against today; returned loans against their return date.
        LocalDate reference = returned ? loan.getReturnDate() : LocalDate.now();
        long overdueDays = Math.max(0L, ChronoUnit.DAYS.between(loan.getDueDate(), reference));
        long fine = returned
                ? loan.getFineAmount()                      // final fine stored at return time
                : calculateFine(loan.getDueDate(), reference); // accruing, recomputed live

        return new LoanResponse(
                loan.getId(),
                loan.getUser().getId(),
                loan.getUser().getName(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                returned ? "RETURNED" : "ACTIVE",
                overdueDays > 0,
                overdueDays,
                fine);
    }
}
