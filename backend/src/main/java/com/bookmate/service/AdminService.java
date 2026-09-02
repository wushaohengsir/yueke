package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final TeacherProfileMapper teacherMapper;
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final SubjectMapper subjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;

    public AdminService(TeacherProfileMapper t, UserMapper u, BookingMapper b,
                        SubjectMapper s, TeacherSubjectMapper ts) {
        this.teacherMapper = t; this.userMapper = u; this.bookingMapper = b;
        this.subjectMapper = s; this.teacherSubjectMapper = ts;
    }

    // ---- 老师审核列表 ----
    public List<Map<String, Object>> listTeachers(Integer auditStatus) {
        LambdaQueryWrapper<TeacherProfile> q = new LambdaQueryWrapper<>();
        if (auditStatus != null) q.eq(TeacherProfile::getAuditStatus, auditStatus);
        List<TeacherProfile> tps = teacherMapper.selectList(q.orderByDesc(TeacherProfile::getId));
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TeacherProfile tp : tps) {
            User u = userMapper.selectById(tp.getUserId());
            List<TeacherSubject> tss = teacherSubjectMapper.selectList(
                    new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, tp.getUserId()));
            String subs = tss.stream().map(ts -> subjById.get(ts.getSubjectId()))
                    .filter(Objects::nonNull).map(Subject::getName).collect(Collectors.joining("、"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", tp.getUserId());
            m.put("name", u != null ? u.getName() : "");
            m.put("phone", u != null ? u.getPhone() : "");
            m.put("title", tp.getTitle());
            m.put("intro", tp.getIntro());
            m.put("subjects", subs);
            m.put("auditStatus", tp.getAuditStatus());
            out.add(m);
        }
        return out;
    }

    // ---- 审核老师 ----
    public boolean auditTeacher(long teacherUserId, boolean approve) {
        TeacherProfile tp = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, teacherUserId));
        if (tp == null || tp.getAuditStatus() != 0) return false;
        tp.setAuditStatus(approve ? 1 : 2);
        teacherMapper.updateById(tp);
        return true;
    }

    // ---- 数据看板 ----
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        Long students = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 1));
        Long teachers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 2));
        Long pendingAudit = teacherMapper.selectCount(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getAuditStatus, 0));
        Long activeBookings = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .in(Booking::getStatus, 0, 1));
        Long completed = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, 2));
        Long leaves = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, 4));
        m.put("students", students);
        m.put("teachers", teachers);
        m.put("pendingAudit", pendingAudit);
        m.put("activeBookings", activeBookings);
        m.put("completed", completed);
        m.put("leaves", leaves);

        // 按科目统计预约
        Map<Long, Subject> subjById = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<Booking> all = bookingMapper.selectList(null);
        Map<String, Long> bySubject = all.stream()
                .filter(b -> b.getSubjectId() != null)
                .collect(Collectors.groupingBy(
                        b -> subjById.containsKey(b.getSubjectId()) ? subjById.get(b.getSubjectId()).getName() : "其他",
                        Collectors.counting()));
        m.put("bookingsBySubject", bySubject);
        return m;
    }
}
