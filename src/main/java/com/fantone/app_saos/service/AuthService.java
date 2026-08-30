package com.fantone.app_saos.service;

import com.fantone.app_saos.dto.request.AuthRequestDto;
import com.fantone.app_saos.dto.request.LoginRequestDto;
import com.fantone.app_saos.dto.response.MessageResponse;
import com.fantone.app_saos.mapper.UserMapper;
import com.fantone.app_saos.model.RefreshToken;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.security.UserDetailsImpl;
import com.fantone.app_saos.service.payload.AuthTokens;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.digest.DigestUtils;

@Service
public class AuthService {
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private UserMapper userMapper;

    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    public MessageResponse register(AuthRequestDto dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));
        userService.create(user);

        return new MessageResponse("Registrazione avvenuta con successo!");
    }

    public AuthTokens login(LoginRequestDto dto) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(dto.email(), dto.password());
        Authentication authentication = authenticationManager.authenticate(token);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        String id = user.getId().toString();

        String accessToken = jwtService.generate(id, user.getRole().name());
        RefreshToken refreshTokenObj = refreshTokenService.generate(user);
        String refreshToken = refreshTokenObj.getToken();

        String hashedToken = DigestUtils.sha256Hex(refreshToken);
        refreshTokenObj.setToken(hashedToken);

        refreshTokenService.save(refreshTokenObj);

        return new AuthTokens(accessToken, refreshToken);
    }

    @Transactional
    public String refresh(String refreshToken) {
        RefreshToken tokenRecord = refreshTokenService.validate(refreshToken);

        User user = tokenRecord.getUser();
        String id = user.getId().toString();

        String accessToken = jwtService.generate(id, user.getRole().name());

        // genera nuovo refresh token
//        String refreshToken = UUID.randomUUID().toString();
//        tokenRecord.setToken(refreshToken);
//        tokenRecord.setExpires_at(LocalDateTime.now().plusDays(30));

        return accessToken;
    }
}
