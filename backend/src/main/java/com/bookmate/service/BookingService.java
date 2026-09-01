package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.dto.SlotView;
import com.bookmate.dto.TeacherView;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public BookingService(BookingMapper b, TeacherProfileMapper t, UserMapper u,
                          SubjectMapper s, TeacherSubjectMapper ts, TimeslotTemplateMapper tm) {
        this.bookingMapper = b; this.teacherMapper = t; this.userMapper = u;
        this.subjectMapper = s; this.teacherSubjectMapper = ts; this.templateMapper = tm;
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

    // 生成未来 14 天时段（模板 - 已约）
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

        List<SlotView> slots = new ArrayList<>();
        LocalDate start = LocalDate.now();
        for (int d = 1; d <= 14; d++) {
            LocalDate date = start.plusDays(d);
            int weekday = date.getDayOfWeek().getValue();
            for (TimeslotTemplate t : templates) {
                if (t.getWeekday() != weekday) continue;
                LocalDateTime s = LocalDateTime.of(date, t.getStartTime());
                LocalDateTime e = LocalDateTime.of(date, t.getEndTime());
                SlotView sv = new SlotView();
                sv.setId(teacherId + "-" + s);
                sv.setStartAt(s); sv.setEndAt(e);
                sv.setStatus(booked.contains(s) ? "booked" : "available");
                slots.add(sv);
            }
        }
        slots.sort(Comparator.comparing(SlotView::getStartAt));
        return slots;
    }

    public List<Booking> listByStudent(long studentId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStudentId, studentId).orderByDesc(Booking::getStartAt));
    }

    @Transactional
    public boolean leave(long bookingId, long studentId) {
        Booking b = bookingMapper.selectById(bookingId);
        if (b == null || !b.getStudentId().equals(studentId) || b.getStatus() != 1) return false;
        b.setStatus(4); // 已请假
        bookingMapper.updateById(b);
        return true;
    }

    @Transactional
    public Booking create(long teacherId, long studentId, LocalDateTime startAt, LocalDateTime endAt) {
        Long cnt = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId)
                .eq(Booking::getStartAt, startAt)
                .in(Booking::getStatus, 0, 1));
        if (cnt != null && cnt > 0) throw new IllegalStateException("该时段已被占用，请选相邻时段");
        Booking b = new Booking();
        b.setTeacherId(teacherId); b.setStudentId(studentId);
        b.setStartAt(startAt); b.setEndAt(endAt); b.setStatus(1);
        try { bookingMapper.insert(b); }
        catch (DuplicateKeyException e) { throw new IllegalStateException("该时段刚被占用，请选相邻时段"); }
        return b;
    }
}
