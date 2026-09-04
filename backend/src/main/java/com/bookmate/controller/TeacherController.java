package com.bookmate.controller;

import com.bookmate.common.AuthHelper;
import com.bookmate.common.OpStatus;
import com.bookmate.common.Result;
import com.bookmate.service.TeacherService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final TeacherService teacherService;
    private final AuthHelper auth;

    public TeacherController(TeacherService t, AuthHelper auth) {
        this.teacherService = t;
        this.auth = auth;
    }

    // 周课表
    @GetMapping("/bookings")
    public Result<?> bookings(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(teacherService.listTeacherBookings(auth.userId(authHeader)));
    }

    // 周课表（按周：weekOffset 0=本周，-1=上周，1=下周；过去读真实 booking，未来读模板+booking）
    @GetMapping("/week-schedule")
    public Result<?> weekSchedule(@RequestHeader("Authorization") String authHeader,
                                  @RequestParam(defaultValue = "0") int weekOffset) {
        return Result.ok(teacherService.listWeekSchedule(auth.userId(authHeader), weekOffset));
    }

    // 登记课时（标记完成；需已过上课结束时间）
    @PostMapping("/bookings/{id}/complete")
    public Result<?> complete(@RequestHeader("Authorization") String authHeader, @PathVariable long id) {
        OpStatus r = teacherService.completeBooking(auth.userId(authHeader), id);
        return switch (r) {
            case OK -> Result.ok(true);
            case NOT_TIME -> Result.fail(400, "课程尚未结束，无法登记完成");
            default -> Result.fail(400, "登记失败（仅已确认课时可登记）");
        };
    }

    // 请假列表
    @GetMapping("/leaves")
    public Result<?> leaves(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(teacherService.listTeacherLeaves(auth.userId(authHeader)));
    }

    // 审批请假
    @PostMapping("/leaves/{id}/handle")
    public Result<?> handle(@RequestHeader("Authorization") String authHeader,
                            @PathVariable long id, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.parseBoolean(String.valueOf(body.get("approve")));
        boolean ok = teacherService.handleLeave(auth.userId(authHeader), id, approve);
        return ok ? Result.ok(true) : Result.fail(400, "处理失败");
    }

    // 时段模板
    @GetMapping("/templates")
    public Result<?> templates(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(teacherService.listTemplates(auth.userId(authHeader)));
    }

    @PostMapping("/templates")
    public Result<?> addTemplate(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        long teacher = auth.userId(authHeader);
        int weekday = Integer.parseInt(String.valueOf(body.get("weekday")));
        String start = String.valueOf(body.get("start"));
        String end = String.valueOf(body.get("end"));
        Long subjectId = body.get("subjectId") != null ? Long.parseLong(String.valueOf(body.get("subjectId"))) : null;
        OpStatus r = teacherService.addTemplate(teacher, weekday, start, end, subjectId);
        if (r == OpStatus.BAD_TIME) return Result.fail(400, "结束时间必须晚于开始时间");
        return Result.ok(true);
    }

    @PostMapping("/templates/{id}/toggle")
    public Result<?> toggle(@PathVariable long id) {
        OpStatus r = teacherService.toggleTemplate(id);
        if (r == OpStatus.CONFLICT) return Result.fail(409, "该时段与已启用的模板冲突，请先停用冲突的模板");
        return Result.ok(true);
    }

    @PutMapping("/templates/{id}")
    public Result<?> updateTemplate(@RequestHeader("Authorization") String authHeader, @PathVariable long id,
                                    @RequestBody Map<String, Object> body) {
        String start = String.valueOf(body.get("start"));
        String end = String.valueOf(body.get("end"));
        OpStatus r = teacherService.updateTemplate(auth.userId(authHeader), id, start, end);
        return switch (r) {
            case NOT_FOUND -> Result.fail(404, "模板不存在或非本人模板");
            case ENABLED -> Result.fail(409, "该模板已启用，请先停用再修改时间");
            case BAD_TIME -> Result.fail(400, "结束时间必须晚于开始时间");
            default -> Result.ok(true);
        };
    }

    @DeleteMapping("/templates/{id}")
    public Result<?> deleteTemplate(@RequestHeader("Authorization") String authHeader, @PathVariable long id) {
        boolean ok = teacherService.deleteTemplate(auth.userId(authHeader), id);
        return ok ? Result.ok(true) : Result.fail(404, "删除失败（模板不存在或非本人模板）");
    }
}
