package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    public boolean existsByUsername(String username);
    public boolean existsByEmail(String email);
    public Optional<User> findByUsername(String username);
    public Optional<User> findByEmail(String email);
    public Optional<User> findById(Long id);
}
