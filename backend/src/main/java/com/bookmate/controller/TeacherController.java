package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.service.TeacherService;
import com.bookmate.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final TeacherService teacherService;
    private final JwtUtil jwtUtil;

    public TeacherController(TeacherService t, JwtUtil j) {
        this.teacherService = t;
        this.jwtUtil = j;
    }

    private long currentUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        return jwtUtil.parseUserId(token);
    }

    // 周课表
    @GetMapping("/bookings")
    public Result<?> bookings(@RequestHeader("Authorization") String auth) {
        return Result.ok(teacherService.listTeacherBookings(currentUserId(auth)));
    }

    // 请假列表
    @GetMapping("/leaves")
    public Result<?> leaves(@RequestHeader("Authorization") String auth) {
        return Result.ok(teacherService.listTeacherLeaves(currentUserId(auth)));
    }

    // 审批请假
    @PostMapping("/leaves/{id}/handle")
    public Result<?> handle(@RequestHeader("Authorization") String auth,
                            @PathVariable long id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.parseBoolean(String.valueOf(body.get("approve")));
        boolean ok = teacherService.handleLeave(currentUserId(auth), id, approve);
        return ok ? Result.ok(true) : Result.fail(400, "处理失败");
    }

    // 时段模板
    @GetMapping("/templates")
    public Result<?> templates(@RequestHeader("Authorization") String auth) {
        return Result.ok(teacherService.listTemplates(currentUserId(auth)));
    }

    @PostMapping("/templates")
    public Result<?> addTemplate(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        long teacher = currentUserId(auth);
        int weekday = Integer.parseInt(String.valueOf(body.get("weekday")));
        String start = String.valueOf(body.get("start"));
        String end = String.valueOf(body.get("end"));
        Long subjectId = body.get("subjectId") != null ? Long.parseLong(String.valueOf(body.get("subjectId"))) : null;
        teacherService.addTemplate(teacher, weekday, start, end, subjectId);
        return Result.ok(true);
    }

    @PostMapping("/templates/{id}/toggle")
    public Result<?> toggle(@PathVariable long id) {
        teacherService.toggleTemplate(id);
        return Result.ok(true);
    }
}
