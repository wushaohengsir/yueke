package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherService {
    private final BookingMapper bookingMapper;
    private final LeaveRequestMapper leaveMapper;
    private final StudentCreditMapper creditMapper;
    private final CreditLogMapper creditLogMapper;
    private final TimeslotTemplateMapper templateMapper;
    private final UserMapper userMapper;
    private final SubjectMapper subjectMapper;

    public TeacherService(BookingMapper b, LeaveRequestMapper l, StudentCreditMapper sc,
                          CreditLogMapper cl, TimeslotTemplateMapper tm, UserMapper u, SubjectMapper s) {
        this.bookingMapper = b; this.leaveMapper = l; this.creditMapper = sc;
        this.creditLogMapper = cl; this.templateMapper = tm; this.userMapper = u; this.subjectMapper = s;
    }

    // ---- 学员提交请假（待审批） ----
    @Transactional
    public boolean submitLeave(long studentId, long bookingId, String reason) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null || !b.getStudentId().equals(studentId) || b.getStatus() != 1) return false;
        Long pending = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getBookingId, bookingId).eq(LeaveRequest::getStatus, 0));
        if (pending != null && pending > 0) return false; // 已有待审批
        LeaveRequest lr = new LeaveRequest();
        lr.setBookingId(bookingId); lr.setStudentId(studentId);
        lr.setReason(reason); lr.setStatus(0);
        leaveMapper.insert(lr);
        return true;
    }

    // ---- 老师：周课表（本周指定星期几的时段 + 预约状态） ----
    public List<Map<String, Object>> listWeekSchedule(long teacherId, int weekday) {
        // 本周该 weekday 对应的具体日期
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate date = monday.plusDays(weekday - 1);

        // 该老师该 weekday 的启用模板
        List<TimeslotTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, teacherId)
                        .eq(TimeslotTemplate::getWeekday, weekday)
                        .eq(TimeslotTemplate::getEnabled, 1)
                        .orderByAsc(TimeslotTemplate::getStartTime));

        // 该老师当天所有预约（活跃 + 已完成）
        List<Booking> dayBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getTeacherId, teacherId)
                        .ge(Booking::getStartAt, date.atStartOfDay())
                        .lt(Booking::getStartAt, date.plusDays(1).atStartOfDay()));

        Map<LocalTime, Booking> bookingByStart = new HashMap<>();
        for (Booking b : dayBookings) {
            bookingByStart.put(b.getStartAt().toLocalTime(), b);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (TimeslotTemplate t : templates) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("startTime", t.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            m.put("endTime", t.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            Booking b = bookingByStart.get(t.getStartTime());
            String status = "free"; // 未预约
            String studentName = "";
            String subjectName = "";
            Long bookingId = null;
            if (b != null) {
                if (b.getStatus() == 2) {
                    status = "completed"; // 已完成
                } else {
                    status = "booked";    // 已预约（待确认/已确认）
                }
                bookingId = b.getId();
                User stu = userMapper.selectById(b.getStudentId());
                studentName = stu != null ? stu.getName() : "";
                if (b.getSubjectId() != null) {
                    Subject s = subjectMapper.selectById(b.getSubjectId());
                    subjectName = s != null ? s.getName() : "";
                }
            }
            m.put("status", status);
            m.put("bookingId", bookingId);
            m.put("studentName", studentName);
            m.put("subjectName", subjectName);
            out.add(m);
        }
        return out;
    }

    // ---- 老师：周课表（带学员/科目名） ----
    public List<Map<String, Object>> listTeacherBookings(long teacherId) {
        List<Booking> bs = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId).orderByAsc(Booking::getStartAt));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Booking b : bs) {
            User stu = userMapper.selectById(b.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("startAt", b.getStartAt());
            m.put("endAt", b.getEndAt());
            m.put("status", b.getStatus());
            m.put("studentName", stu != null ? stu.getName() : "");
            m.put("subjectName", b.getSubjectId() != null && subjById.get(b.getSubjectId()) != null
                    ? subjById.get(b.getSubjectId()).getName() : "");
            out.add(m);
        }
        return out;
    }

    // ---- 老师：登记课时（标记完成） ----
    public boolean completeBooking(long teacherId, long bookingId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null || !b.getTeacherId().equals(teacherId) || b.getStatus() != 1) return false;
        b.setStatus(2); // 已完成
        bookingMapper.updateById(b);
        return true;
    }

    // ---- 老师：请假列表 ----
    public List<Map<String, Object>> listTeacherLeaves(long teacherId) {
        Set<Long> myBookingIds = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId))
                .stream().map(Booking::getId).collect(Collectors.toSet());
        if (myBookingIds.isEmpty()) return List.of();
        List<LeaveRequest> lrs = leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getBookingId, myBookingIds).orderByDesc(LeaveRequest::getCreatedAt));
        Map<Long, Booking> bookingById = bookingMapper.selectBatchIds(myBookingIds).stream()
                .collect(Collectors.toMap(Booking::getId, x -> x));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        List<Map<String, Object>> out = new ArrayList<>();
        for (LeaveRequest lr : lrs) {
            Booking b = bookingById.get(lr.getBookingId());
            User stu = userMapper.selectById(lr.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", lr.getId());
            m.put("bookingId", lr.getBookingId());
            m.put("studentName", stu != null ? stu.getName() : "");
            m.put("startAt", b != null ? b.getStartAt() : null);
            m.put("endAt", b != null ? b.getEndAt() : null);
            m.put("subjectName", b != null && b.getSubjectId() != null && subjById.get(b.getSubjectId()) != null
                    ? subjById.get(b.getSubjectId()).getName() : "");
            m.put("reason", lr.getReason());
            m.put("status", lr.getStatus());
            out.add(m);
        }
        return out;
    }

    // ---- 老师：审批请假 ----
    @Transactional
    public boolean handleLeave(long teacherId, long leaveId, boolean approve) {
        LeaveRequest lr = leaveMapper.selectById(leaveId);
        if (lr == null || lr.getStatus() != 0) return false;
        Booking b = bookingMapper.selectById(lr.getBookingId());
        if (b == null || !b.getTeacherId().equals(teacherId)) return false;

        lr.setHandledBy(teacherId); lr.setHandledAt(LocalDateTime.now());
        if (approve) {
            lr.setStatus(1);
            b.setStatus(4); // 已请假
            bookingMapper.updateById(b);
            if (b.getSubjectId() != null) {
                StudentCredit sc = creditMapper.selectOne(new LambdaQueryWrapper<StudentCredit>()
                        .eq(StudentCredit::getStudentId, b.getStudentId())
                        .eq(StudentCredit::getSubjectId, b.getSubjectId()));
                if (sc != null) {
                    sc.setCreditsUsed(Math.max(0, sc.getCreditsUsed() - 1));
                    creditMapper.updateById(sc);
                    CreditLog log = new CreditLog();
                    log.setStudentId(b.getStudentId()); log.setSubjectId(b.getSubjectId());
                    log.setDelta(1); log.setReason("请假返还"); log.setRefBooking(b.getId());
                    creditLogMapper.insert(log);
                }
            }
        } else {
            lr.setStatus(2); // 驳回，课时保持已确认
        }
        leaveMapper.updateById(lr);
        return true;
    }

    // ---- 老师：时段模板 ----
    public List<TimeslotTemplate> listTemplates(long teacherId) {
        return templateMapper.selectList(new LambdaQueryWrapper<TimeslotTemplate>()
                .eq(TimeslotTemplate::getTeacherId, teacherId).orderByAsc(TimeslotTemplate::getWeekday));
    }

    public void addTemplate(long teacherId, int weekday, String start, String end, Long subjectId) {
        LocalTime st = LocalTime.parse(start);
        LocalTime et = LocalTime.parse(end);
        TimeslotTemplate t = new TimeslotTemplate();
        t.setTeacherId(teacherId); t.setWeekday(weekday);
        t.setStartTime(st);
        t.setEndTime(et);
        t.setSubjectId(subjectId); t.setEnabled(1);
        templateMapper.insert(t);
        // 新模板启用后，停用同天重叠的旧启用模板
        disableOverlapping(teacherId, weekday, st, et, t.getId());
    }

    public void toggleTemplate(long id) {
        TimeslotTemplate t = templateMapper.selectById(id);
        if (t == null) return;
        boolean enable = t.getEnabled() != 1; // 当前停用则本次启用
        t.setEnabled(enable ? 1 : 0);
        templateMapper.updateById(t);
        // 启用时，停用同天重叠的其他启用模板
        if (enable) {
            disableOverlapping(t.getTeacherId(), t.getWeekday(), t.getStartTime(), t.getEndTime(), t.getId());
        }
    }

    // 停用同老师、同星期、时间段重叠的其他已启用模板（同一时段仅一个模板生效）
    private void disableOverlapping(long teacherId, int weekday, LocalTime start, LocalTime end, long excludeId) {
        List<TimeslotTemplate> others = templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, teacherId)
                        .eq(TimeslotTemplate::getWeekday, weekday)
                        .eq(TimeslotTemplate::getEnabled, 1)
                        .ne(TimeslotTemplate::getId, excludeId));
        for (TimeslotTemplate o : others) {
            boolean overlap = start.isBefore(o.getEndTime()) && o.getStartTime().isBefore(end);
            if (overlap) {
                o.setEnabled(0);
                templateMapper.updateById(o);
            }
        }
    }
}
