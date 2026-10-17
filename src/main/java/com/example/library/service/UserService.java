package com.example.library.service;

import com.example.library.dto.UserRequest;
import com.example.library.dto.UserResponse;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.exception.NotFoundException;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    public UserService(UserRepository userRepository, LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("A user with email " + request.email() + " already exists");
        }
        LocalDate joinedAt = request.joinedAt() != null ? request.joinedAt() : LocalDate.now();
        User saved = userRepository.save(
                new User(request.name(), request.email(), request.phone(), joinedAt));
        return toResponse(saved);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Deleting a user with active loans is not allowed; returned-loan history is removed with them. */
    @Transactional
    public void delete(Long id) {
        User user = getExisting(id);
        if (loanRepository.existsByUserIdAndReturnDateIsNull(id)) {
            throw new BusinessException(
                    "User '" + user.getName() + "' has active loans and cannot be deleted");
        }
        loanRepository.deleteByUserId(id); // historical (returned) loans go away with the user
        userRepository.delete(user);
    }

    public User getExisting(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getJoinedAt(), loanRepository.countByUserIdAndReturnDateIsNull(user.getId()));
    }
}
