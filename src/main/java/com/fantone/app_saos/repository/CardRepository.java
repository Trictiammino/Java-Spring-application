package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByUserId(Long userId);
    // CardRepository
    Optional<Card> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);
}
