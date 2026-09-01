package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // ---- 老师：周课表 ----
    public List<Booking> listTeacherBookings(long teacherId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId).orderByAsc(Booking::getStartAt));
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
        TimeslotTemplate t = new TimeslotTemplate();
        t.setTeacherId(teacherId); t.setWeekday(weekday);
        t.setStartTime(java.time.LocalTime.parse(start));
        t.setEndTime(java.time.LocalTime.parse(end));
        t.setSubjectId(subjectId); t.setEnabled(1);
        templateMapper.insert(t);
    }

    public void toggleTemplate(long id) {
        TimeslotTemplate t = templateMapper.selectById(id);
        if (t != null) { t.setEnabled(t.getEnabled() == 1 ? 0 : 1); templateMapper.updateById(t); }
    }
}
