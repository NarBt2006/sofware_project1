package com.example.library;

import com.example.library.dto.BorrowRequest;
import com.example.library.dto.BookRequest;
import com.example.library.dto.UserRequest;
import com.example.library.entity.Book;
import com.example.library.entity.Loan;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.exception.NotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import com.example.library.service.BookService;
import com.example.library.service.LoanService;
import com.example.library.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the borrow/return workflow, business rules and fine
 * calculation. Runs against the default H2 in-memory database (loan period 14
 * days, daily fine 5000 Toman); every test is rolled back afterwards.
 */
@SpringBootTest
@Transactional
class LoanServiceIntegrationTest {

    @Autowired
    LoanService loanService;
    @Autowired
    UserService userService;
    @Autowired
    BookService bookService;
    @Autowired
    UserRepository users;
    @Autowired
    BookRepository books;
    @Autowired
    LoanRepository loans;

    private Long newUser(String email) {
        return userService.create(new UserRequest("Test User", email, null, LocalDate.now())).id();
    }

    private Long newBook(int copies) {
        return bookService.create(new BookRequest("Test Book", "Author", null, copies)).id();
    }

    @Test
    void borrowAndReturnOnTime_noFine_copiesRestored() {
        Long userId = newUser("ontime@example.com");
        Long bookId = newBook(2);

        var loan = loanService.borrow(new BorrowRequest(userId, bookId));
        assertEquals("ACTIVE", loan.status());
        assertEquals(14, loan.dueDate().toEpochDay() - loan.borrowDate().toEpochDay()); // loan period
        assertEquals(1, books.findById(bookId).orElseThrow().getAvailableCopies());

        var returned = loanService.returnBook(loan.id());
        assertEquals("RETURNED", returned.status());
        assertFalse(returned.overdue());
        assertEquals(0, returned.fineAmount());
        assertEquals(2, books.findById(bookId).orElseThrow().getAvailableCopies());
    }

    @Test
    void openOverdueLoan_accruesFineUpToToday() {
        User user = users.save(new User("Late User", "late@example.com", null, LocalDate.now()));
        Book book = books.save(new Book("Old Book", "Author", null, 1, 0));
        // borrowed 20 days ago -> due 6 days ago, still not returned
        loans.save(new Loan(user, book, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6)));

        var view = loanService.findOverdue().stream()
                .filter(l -> l.bookTitle().equals("Old Book"))
                .findFirst().orElseThrow();

        assertTrue(view.overdue());
        assertEquals("ACTIVE", view.status());
        assertEquals(6, view.overdueDays());
        assertEquals(6 * 5000, view.fineAmount()); // 30,000 Toman accruing
    }

    @Test
    void lateReturn_storesFinalFineBasedOnDueDate() {
        User user = users.save(new User("Late User 2", "late2@example.com", null, LocalDate.now()));
        Book book = books.save(new Book("Old Book 3", "Author", null, 1, 0)); // copy is out
        // borrowed 20 days ago -> due 6 days ago
        Loan entity = loans.save(new Loan(user, book,
                LocalDate.now().minusDays(20), LocalDate.now().minusDays(6)));

        var returned = loanService.returnBook(entity.getId());
        assertEquals("RETURNED", returned.status());
        assertTrue(returned.overdue());
        assertEquals(6, returned.overdueDays());
        assertEquals(6 * 5000, returned.fineAmount()); // final fine fixed at return time
        // ...and it is persisted on the loan row
        assertEquals(6 * 5000, loans.findById(entity.getId()).orElseThrow().getFineAmount());
    }

    @Test
    void returnedLoan_reportsStoredFineAndDaysFromReturnDate() {
        User user = users.save(new User("Late User 2", "late3@example.com", null, LocalDate.now()));
        Book book = books.save(new Book("Old Book 2", "Author", null, 1, 1));
        // due 6 days ago, returned 3 days ago, final fine stored: 3 * 5000
        Loan loan = new Loan(user, book, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6));
        loan.setReturnDate(LocalDate.now().minusDays(3));
        loan.setFineAmount(15000L);
        loans.save(loan);

        var view = loanService.findAll(null).stream()
                .filter(l -> l.bookTitle().equals("Old Book 2"))
                .findFirst().orElseThrow();

        assertEquals("RETURNED", view.status());
        assertEquals(3, view.overdueDays()); // counted to the return date, not today
        assertEquals(15000L, view.fineAmount());
    }

    @Test
    void sameUserCannotBorrowSameBookTwiceWithoutReturning() {
        Long userId = newUser("dup@example.com");
        Long bookId = newBook(3);

        loanService.borrow(new BorrowRequest(userId, bookId));

        assertThrows(BusinessException.class,
                () -> loanService.borrow(new BorrowRequest(userId, bookId)));
    }

    @Test
    void cannotBorrowWhenNoCopiesAvailable() {
        Long u1 = newUser("nocopy1@example.com");
        Long u2 = newUser("nocopy2@example.com");
        Long bookId = newBook(1);

        loanService.borrow(new BorrowRequest(u1, bookId));

        assertThrows(BusinessException.class,
                () -> loanService.borrow(new BorrowRequest(u2, bookId)));
    }

    @Test
    void unknownUserBookOrLoan_givesNotFound() {
        Long bookId = newBook(1);
        Long userId = newUser("nf@example.com");

        assertThrows(NotFoundException.class, () -> loanService.borrow(new BorrowRequest(9999L, bookId)));
        assertThrows(NotFoundException.class, () -> loanService.borrow(new BorrowRequest(userId, 9999L)));
        assertThrows(NotFoundException.class, () -> loanService.returnBook(9999L));
    }

    @Test
    void userOrBookWithActiveLoan_cannotBeDeleted() {
        Long userId = newUser("del@example.com");
        Long bookId = newBook(1);

        loanService.borrow(new BorrowRequest(userId, bookId));

        assertThrows(BusinessException.class, () -> userService.delete(userId));
        assertThrows(BusinessException.class, () -> bookService.delete(bookId));
    }
}
