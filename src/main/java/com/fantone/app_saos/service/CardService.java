package com.fantone.app_saos.service;

import com.fantone.app_saos.dto.response.CardDto;
import com.fantone.app_saos.exception.ResourceConflictException;
import com.fantone.app_saos.model.Card;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;

    public CardDto generateCard(Long userId) {
        // Blocca se esiste già una card valida
        cardRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now())
                .ifPresent(c -> { throw new ResourceConflictException("User already has a valid card"); });

        BigDecimal randomBalance = BigDecimal.valueOf(
                ThreadLocalRandom.current().nextDouble(40.0, 500.0)
        ).setScale(2, RoundingMode.HALF_UP);

        User user = userService.findById(userId);

        Card card = new Card();
        card.setBalance(randomBalance);
        card.setUser(user);
        // expires_at e created_at hanno già i default nel model

        Card saved = cardRepository.save(card);

        return new CardDto(
                saved.getId(),
                saved.getBalance(),
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }

    public Optional<CardDto> findByUserId(Long userId) {
        return cardRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now())
                .map(card -> new CardDto(
                        card.getId(),
                        card.getBalance(),
                        card.getExpiresAt(),
                        card.getCreatedAt()
                ));
    }

}