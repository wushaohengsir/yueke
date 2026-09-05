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

    // 登录（纯登录，不再"查无此号自动建档"；待审/驳回老师在此被拒）
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, Object> body) {
        String phone = String.valueOf(body.get("phone"));
        String password = String.valueOf(body.getOrDefault("password", ""));
        if (phone.isBlank()) return Result.fail(400, "请输入手机号");
        if (password.isBlank()) return Result.fail(400, "请输入密码");
        int role = body.get("role") != null ? Integer.parseInt(String.valueOf(body.get("role"))) : 1;
        try {
            return Result.ok(authService.login(phone, password, role));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // 注册（学员/老师；管理员不开公开注册，由启动引导或在任管理员创建）
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> body) {
        int role = body.get("role") != null ? Integer.parseInt(String.valueOf(body.get("role"))) : 1;
        String name = String.valueOf(body.getOrDefault("name", ""));
        String phone = String.valueOf(body.get("phone"));
        String password = String.valueOf(body.getOrDefault("password", ""));
        Long subjectId = body.get("subjectId") != null && !String.valueOf(body.get("subjectId")).isBlank()
                ? Long.parseLong(String.valueOf(body.get("subjectId"))) : null;
        try {
            return Result.ok(authService.register(role, name, phone, password, subjectId));
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
