package com.fantone.app_saos.controller;

import com.fantone.app_saos.exception.RefreshTokenException;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.service.AuthService;
import com.fantone.app_saos.service.JwtService;
import com.fantone.app_saos.service.RefreshTokenService;
import com.fantone.app_saos.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private UserService userService;
    private RefreshTokenService refreshTokenService;
    private JwtService jwtService;
    private AuthService authService;

    public PageController(UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService, AuthService authService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/auth/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/auth/login")
    public String redirectLogin(HttpServletRequest request, HttpServletResponse response) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()&& !(auth instanceof AnonymousAuthenticationToken)) {
            // Utente già loggato → redirect a profile
            return "redirect:/home/profile";
        }

//        // 2. Controlla se ha un refreshToken cookie valido
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if ("refreshToken".equals(cookie.getName())) {
//                    try {
//                        // se refreshToken valido —> genera nuovo accessToken
//                        String accessToken = authService.refresh(cookie.getValue());
//
//                        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
//                                .httpOnly(true)       // Non accessibile da JS
//                                .secure(true)        // True se sei in HTTPS
//                                .path("/")            // Cookie valido per tutto il sito
//                                .sameSite("Lax")
//                                .maxAge(60 * 60)      // 1 ora
//                                .build();
//
//                        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
//
//                        // Puoi metterlo nell'header o in un cookie, dipende da come gestisci il flusso
//                        return "redirect:/home/profile";
//                    } catch (RefreshTokenException e) {
//                        // Token scaduto o non trovato, mostra login normalmente
//                    }
//                }
//            }
//        }

        return "auth/login";
    }

    @GetMapping("/home/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        Long id = Long.valueOf(userId);
        User user = userService.findById(id);

        String username = user.getUsername();

        model.addAttribute("username", username); // passo a Thymeleaf
        return "/home/profile"; // il template profile.html
    }

    @GetMapping("/home/shop")
    public String profile() {
        return "/home/shop"; // il template shop.html
    }

}