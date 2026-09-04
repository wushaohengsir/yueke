package com.bookmate.controller;

import com.bookmate.common.AppTime;
import com.bookmate.common.AuthHelper;
import com.bookmate.common.Result;
import com.bookmate.service.AdminService;
import com.bookmate.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final BookingService bookingService;
    private final AuthHelper auth;

    public AdminController(AdminService a, BookingService b, AuthHelper auth) {
        this.adminService = a;
        this.bookingService = b;
        this.auth = auth;
    }

    // 老师列表（可按审核状态过滤：0待审1通过2驳回）
    @GetMapping("/teachers")
    public Result<?> teachers(@RequestParam(required = false) Integer status) {
        return Result.ok(adminService.listTeachers(status));
    }

    // 审核老师
    @PostMapping("/teachers/{userId}/audit")
    public Result<?> audit(@PathVariable long userId, @RequestBody Map<String, Object> body) {
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

    // ===== 管理员排课：代学生预约未来课程（仅管理员可调）=====

    // 某老师某日期的开放时段（按启用模板 + 该日星期匹配）
    @GetMapping("/plan/slots")
    public Result<?> planSlots(@RequestHeader("Authorization") String authHeader,
                               @RequestParam long teacherId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!auth.hasRole(authHeader, AuthHelper.ROLE_ADMIN)) return Result.fail(403, "无权访问");
        if (date.isBefore(AppTime.today())) {
            return Result.fail(400, "只能安排今天及以后的课程");
        }
        return Result.ok(bookingService.generateSlotsOn(teacherId, date));
    }

    // 管理员为指定学员预约指定老师某时段（时段须在该老师启用模板内）
    @PostMapping("/plan/book")
    public Result<?> planBook(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        if (!auth.hasRole(authHeader, AuthHelper.ROLE_ADMIN)) return Result.fail(403, "无权访问");
        long studentId = Long.parseLong(String.valueOf(body.get("studentId")));
        long teacherId = Long.parseLong(String.valueOf(body.get("teacherId")));
        LocalDateTime start = LocalDateTime.parse(String.valueOf(body.get("startAt")));
        LocalDateTime end = LocalDateTime.parse(String.valueOf(body.get("endAt")));
        if (!adminService.isUserRole(studentId, AuthHelper.ROLE_STUDENT)) return Result.fail(400, "请选择学员账号");
        if (start.isBefore(AppTime.now())) {
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
