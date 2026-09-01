package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService a) { this.authService = a; }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> body) {
        String phone = String.valueOf(body.get("phone"));
        String password = String.valueOf(body.getOrDefault("password", "123456"));
        int role = body.get("role") != null ? Integer.parseInt(String.valueOf(body.get("role"))) : 1;
        try {
            return Result.ok(authService.login(phone, password, role));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
