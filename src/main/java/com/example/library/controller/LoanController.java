package com.example.library.controller;

import com.example.library.dto.BorrowRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** Borrow a book: POST { "userId": 1, "bookId": 2 } */
    @PostMapping
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(request));
    }

    /** Return a book; the final fine is computed and stored here. */
    @PutMapping("/{id}/return")
    public LoanResponse returnBook(@PathVariable Long id) {
        return loanService.returnBook(id);
    }

    /** All loans; ?active=true returns only open (not yet returned) loans. */
    @GetMapping
    public List<LoanResponse> findAll(@RequestParam(name = "active", required = false) Boolean active) {
        return loanService.findAll(active);
    }

    /** Overdue report: open loans past their due date with the fine accrued until today. */
    @GetMapping("/overdue")
    public List<LoanResponse> findOverdue() {
        return loanService.findOverdue();
    }
}
