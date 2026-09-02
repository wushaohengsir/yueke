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

    // 用户管理：列表（可按角色过滤 1学员2老师3管理员）
    @GetMapping("/users")
    public Result<?> users(@RequestParam(required = false) Integer role) {
        return Result.ok(adminService.listUsers(role));
    }

    // 用户管理：禁用/启用
    @PostMapping("/users/{id}/toggle")
    public Result<?> toggleUser(@PathVariable long id, @RequestBody Map<String, Object> body) {
        boolean enable = Boolean.parseBoolean(String.valueOf(body.get("enable")));
        boolean ok = adminService.toggleUser(id, enable);
        return ok ? Result.ok(true) : Result.fail(400, "操作失败");
    }

    // 科目管理：列表
    @GetMapping("/subjects")
    public Result<?> subjects() {
        return Result.ok(adminService.listSubjects());
    }

    // 科目管理：新增
    @PostMapping("/subjects")
    public Result<?> addSubject(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String category = body.getOrDefault("category", "");
        if (name == null || name.isBlank()) return Result.fail(400, "科目名不能为空");
        boolean ok = adminService.addSubject(name, category);
        return ok ? Result.ok(true) : Result.fail(400, "科目已存在");
    }
}
