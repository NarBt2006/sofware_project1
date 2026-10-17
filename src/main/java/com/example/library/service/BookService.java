package com.example.library.service;

import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.exception.NotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        // A brand-new book starts with every copy on the shelf.
        Book book = new Book(request.title(), request.author(), request.isbn(),
                request.totalCopies(), request.totalCopies());
        return toResponse(bookRepository.save(book));
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Deleting a book with active loans is not allowed; returned-loan history is removed with it. */
    @Transactional
    public void delete(Long id) {
        Book book = getExisting(id);
        if (loanRepository.existsByBookIdAndReturnDateIsNull(id)) {
            throw new BusinessException(
                    "Book '" + book.getTitle() + "' has active loans and cannot be deleted");
        }
        loanRepository.deleteByBookId(id); // historical (returned) loans go away with the book
        bookRepository.delete(book);
    }

    public Book getExisting(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book with id " + id + " not found"));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getTotalCopies(), book.getAvailableCopies(),
                book.getAvailableCopies() > 0);
    }
}
