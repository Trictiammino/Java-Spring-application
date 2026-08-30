package com.fantone.app_saos.security;

import com.fantone.app_saos.exception.RefreshTokenException;
import com.fantone.app_saos.service.AuthService;
import com.fantone.app_saos.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private JwtService jwtService;
    private AuthService authService;

    public JwtFilter(JwtService jwtService, @Lazy AuthService authService) {
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filter) throws ServletException, IOException {
        String token = null;
        String refreshToken = null;

        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        if (token == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName()))  token        = cookie.getValue();
                    if ("refreshToken".equals(cookie.getName())) refreshToken = cookie.getValue();
                }
            }
        }

        // accessToken assente o non valido → prova il refresh
        if ((token == null || token.isBlank() || !jwtService.isTokenValid(token)) && refreshToken != null) {
            try {
                token = authService.refresh(refreshToken);

                ResponseCookie newCookie = ResponseCookie.from("accessToken", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(60 * 60)
                        .build();
                res.addHeader(HttpHeaders.SET_COOKIE, newCookie.toString());

            } catch (RefreshTokenException e) {
                // refreshToken scaduto → token rimane null, SecurityContext vuoto → Spring manda al login
            }
        }

        if (token != null && !token.isBlank() && jwtService.isTokenValid(token)) {
            try {
                Claims claims = jwtService.validate(token);
                String sub  = jwtService.extractSubject(claims);
                String role = jwtService.extractRole(claims);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        sub, null, List.of(new SimpleGrantedAuthority(role)));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filter.doFilter(req, res);
    }
}