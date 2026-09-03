package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.dto.CreditView;
import com.bookmate.dto.SlotView;
import com.bookmate.dto.TeacherView;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingMapper bookingMapper;
    private final TeacherProfileMapper teacherMapper;
    private final UserMapper userMapper;
    private final SubjectMapper subjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final TimeslotTemplateMapper templateMapper;
    private final StudentCreditMapper creditMapper;
    private final CreditLogMapper creditLogMapper;

    public BookingService(BookingMapper b, TeacherProfileMapper t, UserMapper u,
                          SubjectMapper s, TeacherSubjectMapper ts, TimeslotTemplateMapper tm,
                          StudentCreditMapper sc, CreditLogMapper cl) {
        this.bookingMapper = b; this.teacherMapper = t; this.userMapper = u;
        this.subjectMapper = s; this.teacherSubjectMapper = ts; this.templateMapper = tm;
        this.creditMapper = sc; this.creditLogMapper = cl;
    }

    public List<TeacherView> listTeachers() {
        List<TeacherProfile> tps = teacherMapper.selectList(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getAuditStatus, 1));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<TeacherView> views = new ArrayList<>();
        for (TeacherProfile tp : tps) {
            User u = userMapper.selectById(tp.getUserId());
            List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                    new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, tp.getUserId()));
            List<String> subs = tss.stream()
                    .map(ts -> subjById.get(ts.getSubjectId()))
                    .filter(Objects::nonNull).map(Subject::getName).collect(Collectors.toList());
            TeacherView v = new TeacherView();
            v.setId(tp.getUserId());
            v.setName(u != null ? u.getName() : "");
            v.setTitle(tp.getTitle());
            v.setIntro(tp.getIntro());
            v.setRating(tp.getRating());
            v.setSubjects(subs);
            views.add(v);
        }
        return views;
    }

    // 生成「明天」的可约时段：明天是周几，就取该老师的周几模板；每个启用模板作为一个完整时段
    public List<SlotView> generateSlots(long teacherId) {
        List<TimeslotTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, teacherId)
                        .eq(TimeslotTemplate::getEnabled, 1));
        Set<LocalDateTime> booked = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getTeacherId, teacherId)
                        .in(Booking::getStatus, 0, 1))
                .stream().map(Booking::getStartAt).collect(Collectors.toSet());

        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1);
        int weekday = tomorrow.getDayOfWeek().getValue();

        List<SlotView> slots = new ArrayList<>();
        for (TimeslotTemplate t : templates) {
            if (t.getWeekday() != weekday) continue;
            LocalDateTime s = LocalDateTime.of(tomorrow, t.getStartTime());
            LocalDateTime e = LocalDateTime.of(tomorrow, t.getEndTime());
            SlotView sv = new SlotView();
            sv.setId(teacherId + "-" + s);
            sv.setStartAt(s); sv.setEndAt(e);
            sv.setStatus(booked.contains(s) ? "booked" : "available");
            slots.add(sv);
        }
        slots.sort(Comparator.comparing(SlotView::getStartAt));
        return slots;
    }

    // 学员分课程课时
    public List<CreditView> getCredits(long studentId) {
        List<StudentCredit> list = creditMapper.selectList(
                new LambdaQueryWrapper<StudentCredit>().eq(StudentCredit::getStudentId, studentId));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<CreditView> views = new ArrayList<>();
        for (StudentCredit sc : list) {
            Subject s = subjById.get(sc.getSubjectId());
            CreditView cv = new CreditView();
            cv.setSubjectId(sc.getSubjectId());
            cv.setSubjectName(s != null ? s.getName() : "");
            cv.setCategory(s != null ? s.getCategory() : "");
            cv.setTotal(sc.getCreditsTotal());
            cv.setUsed(sc.getCreditsUsed());
            cv.setRemaining(sc.getCreditsTotal() - sc.getCreditsUsed());
            views.add(cv);
        }
        return views;
    }

    public List<Booking> listByStudent(long studentId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStudentId, studentId).orderByDesc(Booking::getStartAt));
    }

    private Long primarySubject(long teacherId) {
        List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, teacherId));
        return tss.isEmpty() ? null : tss.get(0).getSubjectId();
    }

    @Transactional
    public Booking create(long teacherId, long studentId, LocalDateTime startAt, LocalDateTime endAt) {
        Long cnt = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId)
                .eq(Booking::getStartAt, startAt)
                .in(Booking::getStatus, 0, 1));
        if (cnt != null && cnt > 0) throw new IllegalStateException("该时段已被占用，请选相邻时段");

        Long subjectId = primarySubject(teacherId);
        StudentCredit sc = creditMapper.selectOne(new LambdaQueryWrapper<StudentCredit>()
                .eq(StudentCredit::getStudentId, studentId)
                .eq(StudentCredit::getSubjectId, subjectId));
        if (sc == null || (sc.getCreditsTotal() - sc.getCreditsUsed()) <= 0) {
            throw new IllegalStateException("该课程课时不足，请先购买对应课程课时包");
        }

        Booking b = new Booking();
        b.setTeacherId(teacherId); b.setStudentId(studentId);
        b.setSubjectId(subjectId);
        b.setStartAt(startAt); b.setEndAt(endAt); b.setStatus(1);
        try { bookingMapper.insert(b); }
        catch (DuplicateKeyException e) { throw new IllegalStateException("该时段刚被占用，请选相邻时段"); }

        sc.setCreditsUsed(sc.getCreditsUsed() + 1);
        creditMapper.updateById(sc);
        CreditLog log = new CreditLog();
        log.setStudentId(studentId); log.setSubjectId(subjectId);
        log.setDelta(-1); log.setReason("约课消耗"); log.setRefBooking(b.getId());
        creditLogMapper.insert(log);
        return b;
    }

    @Transactional
    public boolean leave(long bookingId, long studentId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null || !b.getStudentId().equals(studentId) || b.getStatus() != 1) return false;
        b.setStatus(4);
        bookingMapper.updateById(b);
        if (b.getSubjectId() != null) {
            StudentCredit sc = creditMapper.selectOne(new LambdaQueryWrapper<StudentCredit>()
                    .eq(StudentCredit::getStudentId, studentId)
                    .eq(StudentCredit::getSubjectId, b.getSubjectId()));
            if (sc != null) {
                sc.setCreditsUsed(Math.max(0, sc.getCreditsUsed() - 1));
                creditMapper.updateById(sc);
                CreditLog log = new CreditLog();
                log.setStudentId(studentId); log.setSubjectId(b.getSubjectId());
                log.setDelta(1); log.setReason("请假返还"); log.setRefBooking(bookingId);
                creditLogMapper.insert(log);
            }
        }
        return true;
    }
}
