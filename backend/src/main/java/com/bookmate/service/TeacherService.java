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

    // ---- 老师：周课表（按周，含具体日期；过去读真实 booking，未来读模板+booking） ----
    public Map<String, Object> listWeekSchedule(long teacherId, int weekOffset) {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        // 本周一 + 偏移
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1).plusWeeks(weekOffset);

        // 该老师全部启用模板（用于未来可约预览）
        List<TimeslotTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, teacherId)
                        .eq(TimeslotTemplate::getEnabled, 1));

        // 该老师这一周内的全部 booking
        List<Booking> weekBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getTeacherId, teacherId)
                        .ge(Booking::getStartAt, monday.atStartOfDay())
                        .lt(Booking::getStartAt, monday.plusDays(7).atStartOfDay()));

        // 按日期分组 booking
        Map<LocalDate, List<Booking>> bookingsByDate = new HashMap<>();
        for (Booking b : weekBookings) {
            bookingsByDate.computeIfAbsent(b.getStartAt().toLocalDate(), k -> new ArrayList<>()).add(b);
        }

        List<Map<String, Object>> days = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            LocalDate date = monday.plusDays(d);
            int weekday = date.getDayOfWeek().getValue();
            boolean isPast = date.isBefore(today);

            List<Map<String, Object>> slots = new ArrayList<>();
            if (isPast) {
                // 过去：只读真实 booking 记录
                for (Booking b : bookingsByDate.getOrDefault(date, List.of())) {
                    slots.add(slotOf(b.getStartAt().toLocalTime(), b.getEndAt().toLocalTime(),
                            bookingStatus(b.getStatus()), b));
                }
            } else {
                // 未来（含今天）：模板生成可约空档 + 叠加已预约
                List<Booking> dayBookings = bookingsByDate.getOrDefault(date, List.of());
                Map<LocalTime, Booking> byStart = new HashMap<>();
                for (Booking b : dayBookings) byStart.put(b.getStartAt().toLocalTime(), b);

                for (TimeslotTemplate t : templates) {
                    if (t.getWeekday() != weekday) continue;
                    LocalTime st = t.getStartTime();
                    LocalTime et = t.getEndTime();
                    Booking b = byStart.get(st);
                    if (b != null) {
                        slots.add(slotOf(st, et, bookingStatus(b.getStatus()), b));
                    } else {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("startTime", st.format(DateTimeFormatter.ofPattern("HH:mm")));
                        s.put("endTime", et.format(DateTimeFormatter.ofPattern("HH:mm")));
                        s.put("status", "free");
                        s.put("bookingId", null);
                        s.put("studentName", "");
                        s.put("subjectName", "");
                        slots.add(s);
                    }
                }
                // 未来若存在模板时段之外的预约（如客服代约），一并展示
                Set<LocalTime> templateStarts = templates.stream()
                        .filter(t -> t.getWeekday() == weekday)
                        .map(TimeslotTemplate::getStartTime)
                        .collect(Collectors.toSet());
                for (Booking b : dayBookings) {
                    if (templateStarts.contains(b.getStartAt().toLocalTime())) continue;
                    slots.add(slotOf(b.getStartAt().toLocalTime(), b.getEndAt().toLocalTime(),
                            bookingStatus(b.getStatus()), b));
                }
            }
            slots.sort(Comparator.comparing((Map<String, Object> s) -> String.valueOf(s.get("startTime"))));

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("weekday", weekday);
            day.put("slots", slots);
            days.add(day);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("weekStart", monday.toString());
        out.put("days", days);
        return out;
    }

    private String bookingStatus(int status) {
        return status == 2 ? "completed" : "booked";
    }

    private Map<String, Object> slotOf(LocalTime st, LocalTime et, String status, Booking b) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("startTime", st.format(DateTimeFormatter.ofPattern("HH:mm")));
        s.put("endTime", et.format(DateTimeFormatter.ofPattern("HH:mm")));
        s.put("status", status);
        s.put("bookingId", b.getId());
        User stu = userMapper.selectById(b.getStudentId());
        s.put("studentName", stu != null ? stu.getName() : "");
        if (b.getSubjectId() != null) {
            Subject subj = subjectMapper.selectById(b.getSubjectId());
            s.put("subjectName", subj != null ? subj.getName() : "");
        } else {
            s.put("subjectName", "");
        }
        return s;
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

    // ---- 老师：登记课时（标记完成；需已过上课结束时间） ----
    public String completeBooking(long teacherId, long bookingId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null || !b.getTeacherId().equals(teacherId) || b.getStatus() != 1) return "not_found";
        if (LocalDateTime.now(ZoneId.of("Asia/Shanghai")).isBefore(b.getEndAt())) return "not_time";
        b.setStatus(2); // 已完成
        bookingMapper.updateById(b);
        return "ok";
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

    // 添加模板：结束时间必须晚于开始时间（否则如 20:00-19:00 违反时间规律），返回状态码
    public String addTemplate(long teacherId, int weekday, String start, String end, Long subjectId) {
        LocalTime st = LocalTime.parse(start);
        LocalTime et = LocalTime.parse(end);
        if (!st.isBefore(et)) return "bad_time"; // 结束 <= 开始 一律拒绝（相等也无意义）
        TimeslotTemplate t = new TimeslotTemplate();
        t.setTeacherId(teacherId); t.setWeekday(weekday);
        t.setStartTime(st);
        t.setEndTime(et);
        t.setSubjectId(subjectId);
        t.setEnabled(0); // 默认停用，由老师手动启用
        templateMapper.insert(t);
        return "ok";
    }

    // 启停模板：启用时若与同天已启用的模板时间重叠，则拒绝并返回 conflict
    public String toggleTemplate(long id) {
        TimeslotTemplate t = templateMapper.selectById(id);
        if (t == null) return "not_found";
        boolean enable = t.getEnabled() != 1; // 当前停用则本次启用
        if (enable && hasOverlap(t)) {
            return "conflict";
        }
        t.setEnabled(enable ? 1 : 0);
        templateMapper.updateById(t);
        return "ok";
    }

    // 删除模板（仅能删除自己的模板）
    public boolean deleteTemplate(long teacherId, long id) {
        TimeslotTemplate t = templateMapper.selectById(id);
        if (t == null || !t.getTeacherId().equals(teacherId)) return false;
        templateMapper.deleteById(id);
        return true;
    }

    // 修改模板时间：仅允许停用状态修改（启用中会影响已生成的约课时段，须先停用）；结束仍须晚于开始
    public String updateTemplate(long teacherId, long id, String start, String end) {
        TimeslotTemplate t = templateMapper.selectById(id);
        if (t == null || !t.getTeacherId().equals(teacherId)) return "not_found";
        if (t.getEnabled() == 1) return "enabled"; // 已启用，须先停用
        LocalTime st = LocalTime.parse(start);
        LocalTime et = LocalTime.parse(end);
        if (!st.isBefore(et)) return "bad_time";
        t.setStartTime(st);
        t.setEndTime(et);
        templateMapper.updateById(t);
        return "ok";
    }

    // 是否存在同老师、同星期、时间段重叠且已启用的其他模板
    private boolean hasOverlap(TimeslotTemplate t) {
        List<TimeslotTemplate> others = templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, t.getTeacherId())
                        .eq(TimeslotTemplate::getWeekday, t.getWeekday())
                        .eq(TimeslotTemplate::getEnabled, 1)
                        .ne(TimeslotTemplate::getId, t.getId()));
        for (TimeslotTemplate o : others) {
            boolean overlap = t.getStartTime().isBefore(o.getEndTime())
                    && o.getStartTime().isBefore(t.getEndTime());
            if (overlap) return true;
        }
        return false;
    }
}
