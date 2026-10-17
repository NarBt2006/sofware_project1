package com.example.library.config;

import com.example.library.entity.Book;
import com.example.library.entity.Loan;
import com.example.library.entity.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Seeds demo data on first startup (relative to today, so overdue
 * loans and fines are always demonstrable). Runs only when the DB is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final BookRepository books;
    private final LoanRepository loans;
    private final LibraryProperties properties;

    public DataSeeder(UserRepository users, BookRepository books, LoanRepository loans,
                      LibraryProperties properties) {
        this.users = users;
        this.books = books;
        this.loans = loans;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return; // already seeded
        }

        User ali = users.save(new User("Ali Ahmadi", "ali@example.com", "0912-111-1111", LocalDate.now().minusMonths(8)));
        User sara = users.save(new User("Sara Karimi", "sara@example.com", "0935-222-2222", LocalDate.now().minusMonths(5)));
        User reza = users.save(new User("Reza Moradi", "reza@example.com", null, LocalDate.now().minusMonths(1)));

        Book cleanCode = books.save(new Book("Clean Code", "Robert C. Martin", "978-0132350884", 3, 3));
        Book effectiveJava = books.save(new Book("Effective Java", "Joshua Bloch", "978-0134685991", 2, 2));
        Book pragmatic = books.save(new Book("The Pragmatic Programmer", "Andrew Hunt", "978-0201616224", 2, 2));
        Book ddd = books.save(new Book("Domain-Driven Design", "Eric Evans", null, 1, 1));

        // 1) Sara: borrowed 10 days ago, returned 2 days ago -> on time, no fine
        seedLoan(sara, pragmatic, 10, 2);

        // 2) Ali: borrowed 20 days ago (due 6 days ago), returned 3 days ago -> 3 days late
        seedLoan(ali, cleanCode, 20, 3);

        // 3) Sara: borrowed 30 days ago (due 16 days ago), returned 5 days ago -> 11 days late
        seedLoan(sara, cleanCode, 30, 5);

        // 4) Reza: borrowed 20 days ago, still out -> 6 days overdue, fine keeps growing
        seedLoan(reza, effectiveJava, 20, null);

        // 5) Ali: borrowed 5 days ago, still out -> on time (due in 9 days)
        seedLoan(ali, ddd, 5, null);
    }

    /**
     * Creates a loan borrowed {@code borrowedDaysAgo} days before today.
     * If {@code returnedDaysAgo} is null the loan stays open (a copy is taken off the shelf);
     * otherwise the book was returned that many days ago and the final fine is stored.
     */
    private void seedLoan(User user, Book book, int borrowedDaysAgo, Integer returnedDaysAgo) {
        LocalDate borrowDate = LocalDate.now().minusDays(borrowedDaysAgo);
        Loan loan = new Loan(user, book, borrowDate, borrowDate.plusDays(properties.loanPeriodDays()));

        if (returnedDaysAgo != null) {
            LocalDate returnDate = LocalDate.now().minusDays(returnedDaysAgo);
            loan.setReturnDate(returnDate);
            long overdue = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
            loan.setFineAmount(Math.max(0, overdue) * properties.fine().dailyAmount());
        } else {
            book.setAvailableCopies(book.getAvailableCopies() - 1); // copy is out
        }
        loans.save(loan);
    }
}
