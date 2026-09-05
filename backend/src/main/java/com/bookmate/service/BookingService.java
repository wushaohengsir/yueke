package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.common.AppTime;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingMapper bookingMapper;
    private final TeacherProfileMapper teacherMapper;
    private final UserMapper userMapper;
    private final TimeslotTemplateMapper templateMapper;
    private final SubjectService subjectService;
    private final CreditService creditService;
    private final BlockService blockService;

    public BookingService(BookingMapper b, TeacherProfileMapper t, UserMapper u,
                          TimeslotTemplateMapper tm, SubjectService ss, CreditService cs,
                          BlockService bs) {
        this.bookingMapper = b; this.teacherMapper = t; this.userMapper = u;
        this.templateMapper = tm; this.subjectService = ss; this.creditService = cs;
        this.blockService = bs;
    }

    public List<TeacherView> listTeachers() {
        List<TeacherProfile> tps = teacherMapper.selectList(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getAuditStatus, 1));
        List<TeacherView> views = new ArrayList<>();
        for (TeacherProfile tp : tps) {
            User u = userMapper.selectById(tp.getUserId());
            TeacherView v = new TeacherView();
            v.setId(tp.getUserId());
            v.setName(u != null ? u.getName() : "");
            v.setTitle(tp.getTitle());
            v.setIntro(tp.getIntro());
            v.setRating(tp.getRating());
            v.setSubjects(subjectService.ofTeacher(tp.getUserId()).stream()
                    .map(Subject::getName).collect(Collectors.toList()));
            views.add(v);
        }
        return views;
    }

    // 生成可约时段：以晚上 21:00 为界滚动——当前 <21:00 显示今天，>=21:00 显示明天
    public List<SlotView> generateSlots(long teacherId) {
        LocalDateTime now = AppTime.now();
        LocalDate target = now.toLocalTime().isBefore(LocalTime.of(21, 0))
                ? now.toLocalDate() : now.toLocalDate().plusDays(1);
        return slotsOn(teacherId, target, false);
    }

    // 生成「指定日期」该老师的可约时段（管理员排课用）：剔除已开始/已过时段
    public List<SlotView> generateSlotsOn(long teacherId, LocalDate date) {
        return slotsOn(teacherId, date, true);
    }

    // 某日期可约时段：仅启用模板、按 date 星期匹配；已占用标 booked；excludePast 时剔除未开始
    private List<SlotView> slotsOn(long teacherId, LocalDate date, boolean excludePast) {
        LocalDateTime now = AppTime.now();
        Set<LocalDateTime> booked = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getTeacherId, teacherId)
                        .in(Booking::getStatus, 0, 1))
                .stream().map(Booking::getStartAt)
                .filter(s -> s.toLocalDate().equals(date)).collect(Collectors.toSet());

        int weekday = date.getDayOfWeek().getValue();
        List<TimeslotBlock> blocks = blockService.blocksOn(teacherId, date); // 当日停课
        List<SlotView> slots = new ArrayList<>();
        for (TimeslotTemplate t : enabledTemplates(teacherId)) {
            if (t.getWeekday() != weekday) continue;
            LocalDateTime s = LocalDateTime.of(date, t.getStartTime());
            LocalDateTime e = LocalDateTime.of(date, t.getEndTime());
            if (excludePast && !s.isAfter(now)) continue; // 已开始/已过的时段不可再排
            if (blockService.overlaps(blocks, t.getStartTime(), t.getEndTime())) continue; // 该时段已停课
            SlotView sv = new SlotView();
            sv.setId(teacherId + "-" + s);
            sv.setStartAt(s); sv.setEndAt(e);
            sv.setStatus(booked.contains(s) ? "booked" : "available");
            slots.add(sv);
        }
        slots.sort(Comparator.comparing(SlotView::getStartAt));
        return slots;
    }

    // 该 startAt/endAt 是否为该老师某条「启用模板」的精确时段（星期+起止一致）
    public boolean isOpenTemplateSlot(long teacherId, LocalDateTime startAt, LocalDateTime endAt) {
        for (TimeslotTemplate t : enabledTemplates(teacherId)) {
            if (t.getWeekday() == startAt.getDayOfWeek().getValue()
                    && t.getStartTime().equals(startAt.toLocalTime())
                    && t.getEndTime().equals(endAt.toLocalTime())) return true;
        }
        return false;
    }

    private List<TimeslotTemplate> enabledTemplates(long teacherId) {
        return templateMapper.selectList(
                new LambdaQueryWrapper<TimeslotTemplate>()
                        .eq(TimeslotTemplate::getTeacherId, teacherId)
                        .eq(TimeslotTemplate::getEnabled, 1));
    }

    public List<Booking> listByStudent(long studentId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStudentId, studentId).orderByDesc(Booking::getStartAt));
    }

    @Transactional
    public Booking create(long teacherId, long studentId, LocalDateTime startAt, LocalDateTime endAt) {
        Long cnt = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getTeacherId, teacherId)
                .eq(Booking::getStartAt, startAt)
                .in(Booking::getStatus, 0, 1));
        if (cnt != null && cnt > 0) throw new IllegalStateException("该时段已被占用，请选相邻时段");

        Long subjectId = creditService.primarySubjectId(teacherId);
        creditService.requireRemaining(studentId, subjectId); // 课时预检，不足直接 409

        Booking b = new Booking();
        b.setTeacherId(teacherId); b.setStudentId(studentId);
        b.setSubjectId(subjectId);
        b.setStartAt(startAt); b.setEndAt(endAt); b.setStatus(1);
        try { bookingMapper.insert(b); }
        catch (DuplicateKeyException e) { throw new IllegalStateException("该时段刚被占用，请选相邻时段"); }

        creditService.consume(studentId, subjectId, b.getId());
        return b;
    }
}
