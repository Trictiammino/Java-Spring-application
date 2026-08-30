package com.fantone.app_saos.service;

import com.fantone.app_saos.exception.RefreshTokenException;
import com.fantone.app_saos.model.RefreshToken;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private RefreshTokenRepository refreshTokenRepo;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    public void save(RefreshToken refreshToken) {
        refreshTokenRepo.save(refreshToken);
    }

    //Recommended to hash the refresh token before storing it in db
    //in case the db gets hacked, hackers cant use it to generate access tokens.
    public RefreshToken generate(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshToken;
    }

    public RefreshToken validate(String refreshToken) {

        String hashedToken =
                DigestUtils.sha256Hex(refreshToken);

        RefreshToken tokenRecord = refreshTokenRepo.findTokenByToken(hashedToken)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

        LocalDateTime currentDateTime = LocalDateTime.now();
        if(tokenRecord.getExpires_at().isBefore(currentDateTime)) {
            throw new RefreshTokenException("Refresh token is expired.");
        }

        return tokenRecord;
    }

    @Transactional
    public void deleteByUserId(Long id) {
        refreshTokenRepo.deleteAllByUserId(id);
    }

}
