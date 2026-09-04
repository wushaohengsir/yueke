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

    // 周课表（按周：weekOffset 0=本周，-1=上周，1=下周；过去读真实 booking，未来读模板+booking）
    @GetMapping("/week-schedule")
    public Result<?> weekSchedule(@RequestHeader("Authorization") String auth,
                                  @RequestParam(defaultValue = "0") int weekOffset) {
        return Result.ok(teacherService.listWeekSchedule(currentUserId(auth), weekOffset));
    }

    // 登记课时（标记完成；需已过上课结束时间）
    @PostMapping("/bookings/{id}/complete")
    public Result<?> complete(@RequestHeader("Authorization") String auth, @PathVariable long id) {
        String r = teacherService.completeBooking(currentUserId(auth), id);
        if ("ok".equals(r)) return Result.ok(true);
        if ("not_time".equals(r)) return Result.fail(400, "课程尚未结束，无法登记完成");
        return Result.fail(400, "登记失败（仅已确认课时可登记）");
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
        String r = teacherService.addTemplate(teacher, weekday, start, end, subjectId);
        if ("bad_time".equals(r)) return Result.fail(400, "结束时间必须晚于开始时间");
        return Result.ok(true);
    }

    @PostMapping("/templates/{id}/toggle")
    public Result<?> toggle(@RequestHeader("Authorization") String auth, @PathVariable long id) {
        String r = teacherService.toggleTemplate(id);
        if ("conflict".equals(r)) return Result.fail(409, "该时段与已启用的模板冲突，请先停用冲突的模板");
        return Result.ok(true);
    }

    @PutMapping("/templates/{id}")
    public Result<?> updateTemplate(@RequestHeader("Authorization") String auth, @PathVariable long id,
                                    @RequestBody Map<String, Object> body) {
        String start = String.valueOf(body.get("start"));
        String end = String.valueOf(body.get("end"));
        String r = teacherService.updateTemplate(currentUserId(auth), id, start, end);
        if ("not_found".equals(r)) return Result.fail(404, "模板不存在或非本人模板");
        if ("enabled".equals(r)) return Result.fail(409, "该模板已启用，请先停用再修改时间");
        if ("bad_time".equals(r)) return Result.fail(400, "结束时间必须晚于开始时间");
        return Result.ok(true);
    }

    @DeleteMapping("/templates/{id}")
    public Result<?> deleteTemplate(@RequestHeader("Authorization") String auth, @PathVariable long id) {
        boolean ok = teacherService.deleteTemplate(currentUserId(auth), id);
        return ok ? Result.ok(true) : Result.fail(404, "删除失败（模板不存在或非本人模板）");
    }
}
