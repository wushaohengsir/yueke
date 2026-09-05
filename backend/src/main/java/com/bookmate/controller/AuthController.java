package com.bookmate.controller;

import com.bookmate.common.AuthHelper;
import com.bookmate.common.Result;
import com.bookmate.service.AuthService;
import com.bookmate.service.SubjectService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SubjectService subjectService;
    private final AuthHelper auth;

    public AuthController(AuthService a, SubjectService s, AuthHelper auth) {
        this.authService = a;
        this.subjectService = s;
        this.auth = auth;
    }

    // 当前登录用户资料（token 换真实姓名/手机号）
    @GetMapping("/me")
    public Result<?> me(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(authService.me(auth.userId(authHeader)));
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> body) {
        String phone = String.valueOf(body.get("phone"));
        String password = String.valueOf(body.getOrDefault("password", ""));
        if (password.isBlank()) return Result.fail(400, "请输入密码");
        int role = body.get("role") != null ? Integer.parseInt(String.valueOf(body.get("role"))) : 1;
        Long subjectId = body.get("subjectId") != null && !String.valueOf(body.get("subjectId")).isBlank()
                ? Long.parseLong(String.valueOf(body.get("subjectId"))) : null;
        String name = body.get("name") != null ? String.valueOf(body.get("name")) : null;
        try {
            return Result.ok(authService.login(phone, password, role, subjectId, name));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // 公开科目列表（老师注册时选择，管理员维护）
    @GetMapping("/subjects")
    public Result<?> subjects() {
        return Result.ok(subjectService.listAll());
    }
}
