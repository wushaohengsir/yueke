package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.service.AdminService;
import com.bookmate.service.BookingService;
import com.bookmate.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    public AdminController(AdminService a, BookingService b, JwtUtil j) {
        this.adminService = a;
        this.bookingService = b;
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

    // ===== 管理员排课：代学生预约未来课程（仅 role=3 可调）=====

    private boolean isAdmin(String auth) {
        try {
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : auth;
            Integer role = jwtUtil.parseRole(token);
            return role != null && role == 3;
        } catch (Exception e) {
            return false;
        }
    }

    // 某老师某日期的开放时段（按启用模板 + 该日星期匹配）
    @GetMapping("/plan/slots")
    public Result<?> planSlots(@RequestHeader("Authorization") String auth,
                               @RequestParam long teacherId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!isAdmin(auth)) return Result.fail(403, "无权访问");
        if (date.isBefore(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
            return Result.fail(400, "只能安排今天及以后的课程");
        }
        return Result.ok(bookingService.generateSlotsOn(teacherId, date));
    }

    // 管理员为指定学员预约指定老师某时段（时段须在该老师启用模板内）
    @PostMapping("/plan/book")
    public Result<?> planBook(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        if (!isAdmin(auth)) return Result.fail(403, "无权访问");
        long studentId = Long.parseLong(String.valueOf(body.get("studentId")));
        long teacherId = Long.parseLong(String.valueOf(body.get("teacherId")));
        LocalDateTime start = LocalDateTime.parse(String.valueOf(body.get("startAt")));
        LocalDateTime end = LocalDateTime.parse(String.valueOf(body.get("endAt")));
        if (!adminService.isUserRole(studentId, 1)) return Result.fail(400, "请选择学员账号");
        if (start.isBefore(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))) {
            return Result.fail(400, "只能安排尚未开始的时段");
        }
        if (!bookingService.isOpenTemplateSlot(teacherId, start, end)) {
            return Result.fail(400, "所选时段不在该老师开放时段内");
        }
        try {
            return Result.ok(bookingService.create(teacherId, studentId, start, end));
        } catch (IllegalStateException e) {
            return Result.fail(409, e.getMessage());
        }
    }
}
