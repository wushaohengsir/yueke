package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.service.AdminService;
import com.bookmate.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService a, JwtUtil j) {
        this.adminService = a;
        this.jwtUtil = j;
    }

    private long currentUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        return jwtUtil.parseUserId(token);
    }

    // 老师列表（可按审核状态过滤：0待审1通过2驳回）
    @GetMapping("/teachers")
    public Result<?> teachers(@RequestParam(required = false) Integer status) {
        return Result.ok(adminService.listTeachers(status));
    }

    // 审核老师
    @PostMapping("/teachers/{userId}/audit")
    public Result<?> audit(@RequestHeader("Authorization") String auth,
                           @PathVariable long userId, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.parseBoolean(String.valueOf(body.get("approve")));
        boolean ok = adminService.auditTeacher(userId, approve);
        return ok ? Result.ok(true) : Result.fail(400, "审核失败（仅待审核状态可操作）");
    }

    // 数据看板
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        return Result.ok(adminService.dashboard());
    }
}
