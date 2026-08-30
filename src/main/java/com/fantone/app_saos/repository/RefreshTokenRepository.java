package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    public Optional<RefreshToken> findTokenByToken(String token);
    public void deleteByToken(String token);
    public void deleteAllByUserId(Long userId);
}
