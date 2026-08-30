package com.fantone.app_saos.service;

import com.fantone.app_saos.exception.ResourceConflictException;
import com.fantone.app_saos.model.Role;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private UserRepository userRepo;
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(User user) {
        if(userRepo.existsByUsername(user.getUsername())) {
            throw new ResourceConflictException("Username already taken");
        }

        if(userRepo.existsByEmail(user.getEmail())) {
            throw new ResourceConflictException("Email already registered");
        }

        user.setRole(Role.ROLE_USER); // default role

        return userRepo.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Transactional
    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceConflictException("User not found with id: " + id));
    }

    public Optional<User> findByEmail(String email) {return userRepo.findByEmail(email);
    }
}
