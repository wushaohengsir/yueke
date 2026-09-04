package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final TeacherProfileMapper teacherMapper;
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final SubjectMapper subjectMapper;
    private final SubjectService subjectService;

    public AdminService(TeacherProfileMapper t, UserMapper u, BookingMapper b,
                        SubjectMapper s, SubjectService ss) {
        this.teacherMapper = t; this.userMapper = u; this.bookingMapper = b;
        this.subjectMapper = s; this.subjectService = ss;
    }

    // 判断某用户是否指定角色（1学员2老师3管理员）
    public boolean isUserRole(long userId, int role) {
        User u = userMapper.selectById(userId);
        return u != null && u.getRole() != null && u.getRole().intValue() == role;
    }

    // ---- 老师审核列表 ----
    public List<Map<String, Object>> listTeachers(Integer auditStatus) {
        LambdaQueryWrapper<TeacherProfile> q = new LambdaQueryWrapper<>();
        if (auditStatus != null) q.eq(TeacherProfile::getAuditStatus, auditStatus);
        List<TeacherProfile> tps = teacherMapper.selectList(q.orderByDesc(TeacherProfile::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TeacherProfile tp : tps) {
            User u = userMapper.selectById(tp.getUserId());
            String subs = subjectService.ofTeacher(tp.getUserId()).stream()
                    .map(Subject::getName).collect(Collectors.joining("、"));
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
        Map<Long, Subject> subjById = subjectService.allById();
        List<Booking> all = bookingMapper.selectList(null);
        Map<String, Long> bySubject = all.stream()
                .filter(b -> b.getSubjectId() != null)
                .collect(Collectors.groupingBy(
                        b -> subjById.containsKey(b.getSubjectId()) ? subjById.get(b.getSubjectId()).getName() : "其他",
                        Collectors.counting()));
        m.put("bookingsBySubject", bySubject);
        return m;
    }

    // ---- 用户管理：按角色列用户 ----
    public List<Map<String, Object>> listUsers(Integer role) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (role != null) q.eq(User::getRole, role);
        List<User> us = userMapper.selectList(q.orderByDesc(User::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : us) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            out.add(m);
        }
        return out;
    }

    // ---- 用户管理：禁用/启用 ----
    public boolean toggleUser(long userId, boolean enable) {
        User u = userMapper.selectById(userId);
        if (u == null) return false;
        u.setStatus(enable ? 1 : 0);
        userMapper.updateById(u);
        return true;
    }

    // ---- 科目管理：列表 ----
    public List<Map<String, Object>> listSubjects() {
        List<Subject> ss = subjectMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Subject s : ss) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("category", s.getCategory());
            out.add(m);
        }
        return out;
    }

    // ---- 科目管理：新增 ----
    public boolean addSubject(String name, String category) {
        Long cnt = subjectMapper.selectCount(new LambdaQueryWrapper<Subject>().eq(Subject::getName, name));
        if (cnt != null && cnt > 0) return false; // 重名
        Subject s = new Subject();
        s.setName(name); s.setCategory(category);
        subjectMapper.insert(s);
        return true;
    }
}
