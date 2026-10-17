package com.example.library.controller;

import com.example.library.dto.LoanResponse;
import com.example.library.dto.UserFineResponse;
import com.example.library.dto.UserRequest;
import com.example.library.dto.UserResponse;
import com.example.library.service.LoanService;
import com.example.library.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final LoanService loanService;

    public UserController(UserService userService, LoanService loanService) {
        this.userService = userService;
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Loan history of one user; ?active=true filters to open loans only. */
    @GetMapping("/{id}/loans")
    public List<LoanResponse> loansOf(@PathVariable Long id,
            @RequestParam(name = "active", required = false) Boolean active) {
        return loanService.findByUser(id, active);
    }

    /** Fine report for one user. */
    @GetMapping("/{id}/fines")
    public UserFineResponse finesOf(@PathVariable Long id) {
        return loanService.userFines(id);
    }
}
