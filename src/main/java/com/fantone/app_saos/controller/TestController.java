package com.fantone.app_saos.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/wco")
    public String test() {
        return "working";
    }

    @GetMapping("/v1")
    @PreAuthorize("hasRole('ADMIN')")
    public String test1(@AuthenticationPrincipal String userId) {
        return userId;
    }
}
