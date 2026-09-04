package com.bookmate.controller;

import com.bookmate.common.AuthHelper;
import com.bookmate.common.Result;
import com.bookmate.service.BookingService;
import com.bookmate.service.CreditService;
import com.bookmate.service.TeacherService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookingService;
    private final TeacherService teacherService;
    private final CreditService creditService;
    private final AuthHelper auth;

    public BookingController(BookingService b, TeacherService t, CreditService c, AuthHelper auth) {
        this.bookingService = b;
        this.teacherService = t;
        this.creditService = c;
        this.auth = auth;
    }

    @GetMapping("/teachers")
    public Result<?> teachers() {
        return Result.ok(bookingService.listTeachers());
    }

    @GetMapping("/teachers/{id}/slots")
    public Result<?> slots(@PathVariable long id) {
        return Result.ok(bookingService.generateSlots(id));
    }

    @GetMapping("/credits")
    public Result<?> credits(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(creditService.viewsOf(auth.userId(authHeader)));
    }

    @GetMapping("/bookings")
    public Result<?> bookings(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(bookingService.listByStudent(auth.userId(authHeader)));
    }

    @PostMapping("/bookings")
    public Result<?> create(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> body) {
        long student = auth.userId(authHeader);
        long teacher = Long.parseLong(body.get("teacherId"));
        LocalDateTime start = LocalDateTime.parse(body.get("startAt"));
        LocalDateTime end = LocalDateTime.parse(body.get("endAt"));
        try {
            return Result.ok(bookingService.create(teacher, student, start, end));
        } catch (IllegalStateException e) {
            return Result.fail(409, e.getMessage());
        }
    }

    @PostMapping("/leave")
    public Result<?> leave(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        long student = auth.userId(authHeader);
        long bookingId = Long.parseLong(String.valueOf(body.get("bookingId")));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        boolean ok = teacherService.submitLeave(student, bookingId, reason);
        return ok ? Result.ok(true) : Result.fail(400, "请假提交失败（仅已确认课时可请假，且不可重复提交）");
    }
}
