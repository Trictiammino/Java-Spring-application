package com.fantone.app_saos.controller;

import com.fantone.app_saos.dto.request.AuthRequestDto;
import com.fantone.app_saos.dto.request.LoginRequestDto;
import com.fantone.app_saos.dto.response.MessageResponse;
import com.fantone.app_saos.service.AuthService;
import com.fantone.app_saos.service.RefreshTokenService;
import com.fantone.app_saos.service.payload.AuthTokens;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private AuthService authService;
    private RefreshTokenService refreshTokenService;
    private final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid AuthRequestDto dto, HttpServletResponse response) {

        MessageResponse res = authService.register(dto);

        // 🔴 LOGOUT AUTOMATICO: elimina cookie vecchi
        ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();

        ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login (@RequestBody @Valid LoginRequestDto dto, HttpServletResponse response, @RequestHeader(value = "Accept", defaultValue = "text/html") String accept) {
        AuthTokens tokens = authService.login(dto);

        // Cookie per l'access token
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", tokens.accessToken())
                .httpOnly(true)       // Non accessibile da JS
                .secure(true)        // True se sei in HTTPS
                .path("/")            // Cookie valido per tutto il sito
                .sameSite("Lax")
                .maxAge(60 * 15)      // 15 minuti
                .build();


        ResponseCookie refreshTokenCookie  = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(30 * 24 * 60 * 60) // ad esempio 30 giorni
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new MessageResponse("Login effettato"));
    }

    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal String userId, HttpServletResponse response) {

        refreshTokenService.deleteByUserId(Long.valueOf(userId));


        // Sovrascrive il cookie con valore vuoto e scadenza immediata
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return "/";
    }

    @PostMapping("/refresh")
    public ResponseEntity<MessageResponse> refresh(@CookieValue(name = "refreshToken") String refreshToken, HttpServletResponse response) {
        String accessToken = authService.refresh(refreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)       // Non accessibile da JS
                .secure(true)        // True se sei in HTTPS
                .path("/")            // Cookie valido per tutto il sito
                .sameSite("Lax")
                .maxAge(60 * 60)      // 1 ora
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("Refresh effettuato!"));
    }


}