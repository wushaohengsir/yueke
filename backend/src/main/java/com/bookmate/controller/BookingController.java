package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.service.BookingService;
import com.bookmate.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    public BookingController(BookingService b, JwtUtil j) {
        this.bookingService = b;
        this.jwtUtil = j;
    }

    private long currentUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        return jwtUtil.parseUserId(token);
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
    public Result<?> credits(@RequestHeader("Authorization") String auth) {
        return Result.ok(bookingService.getCredits(currentUserId(auth)));
    }

    @GetMapping("/bookings")
    public Result<?> bookings(@RequestHeader("Authorization") String auth) {
        return Result.ok(bookingService.listByStudent(currentUserId(auth)));
    }

    @PostMapping("/bookings")
    public Result<?> create(@RequestHeader("Authorization") String auth, @RequestBody Map<String, String> body) {
        long student = currentUserId(auth);
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
    public Result<?> leave(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        long student = currentUserId(auth);
        long bookingId = Long.parseLong(String.valueOf(body.get("bookingId")));
        boolean ok = bookingService.leave(bookingId, student);
        return ok ? Result.ok(true) : Result.fail(400, "请假失败（仅已确认课时可请假）");
    }
}
