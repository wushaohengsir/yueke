package com.bookmate.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.*;
import com.bookmate.mapper.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherMapper;
    private final StudentProfileMapper studentMapper;
    private final SubjectMapper subjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final TimeslotTemplateMapper templateMapper;
    private final StudentCreditMapper creditMapper;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(UserMapper u, TeacherProfileMapper t, StudentProfileMapper s,
                          SubjectMapper sj, TeacherSubjectMapper ts, TimeslotTemplateMapper tm,
                          StudentCreditMapper sc, PasswordEncoder e) {
        this.userMapper = u; this.teacherMapper = t; this.studentMapper = s;
        this.subjectMapper = sj; this.teacherSubjectMapper = ts; this.templateMapper = tm;
        this.creditMapper = sc; this.encoder = e;
    }

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) return; // 已有数据则不重复注入

        // 科目
        Subject s1 = subj("钢琴", "音乐"); Subject s2 = subj("羽毛球", "体育"); Subject s3 = subj("编程", "科技");

        // 老师
        long t1 = teacher("13800000001", "王老师", "钢琴十级", "中央音乐学院毕业，10 年钢琴教学经验。", 4.9, s1);
        long t2 = teacher("13800000002", "李老师", "羽毛球教练", "前省队队员，擅长青少年启蒙。", 4.8, s2);
        long t3 = teacher("13800000003", "张老师", "编程讲师", "一线工程师，主讲 Python / 前端入门。", 4.7, s3);
        // 待审核老师（演示管理端审核流）
        pendingTeacher("13800000004", "赵老师", "声乐教师", "音乐学院声乐专业，待审核演示。", s1);

        // 时段模板（weekday 1-7）
        tpl(t1, 2, LocalTime.of(18,0), LocalTime.of(21,0), s1);
        tpl(t1, 3, LocalTime.of(18,0), LocalTime.of(21,0), s1);
        tpl(t2, 4, LocalTime.of(18,0), LocalTime.of(20,0), s2);
        tpl(t3, 5, LocalTime.of(19,0), LocalTime.of(21,0), s3);
        tpl(t3, 6, LocalTime.of(10,0), LocalTime.of(12,0), s3);

        // 学员
        User stu = new User();
        stu.setPhone("13800000000"); stu.setName("学员小约"); stu.setRole(1); stu.setStatus(1);
        stu.setPasswordHash(encoder.encode("123456"));
        userMapper.insert(stu);
        StudentProfile sp = new StudentProfile();
        sp.setUserId(stu.getId()); sp.setCreditsTotal(0); sp.setCreditsUsed(0);
        studentMapper.insert(sp);

        // 分课程课时（不通用）
        credit(stu.getId(), s1, 10); // 钢琴 10 节
        credit(stu.getId(), s2, 5);  // 羽毛球 5 节
        credit(stu.getId(), s3, 3);  // 编程 3 节

        // 管理员
        User admin = new User();
        admin.setPhone("13900000000"); admin.setName("管理员"); admin.setRole(3); admin.setStatus(1);
        admin.setPasswordHash(encoder.encode("123456"));
        userMapper.insert(admin);

        System.out.println("[DemoDataSeeder] seeded 4 teachers(1 pending) + 1 student + 1 admin");
    }

    private void credit(long studentId, Subject s, int total) {
        StudentCredit sc = new StudentCredit();
        sc.setStudentId(studentId); sc.setSubjectId(s.getId());
        sc.setCreditsTotal(total); sc.setCreditsUsed(0);
        creditMapper.insert(sc);
    }

    private void pendingTeacher(String phone, String name, String title, String intro, Subject s) {
        User u = new User();
        u.setPhone(phone); u.setName(name); u.setRole(2); u.setStatus(1);
        u.setPasswordHash(encoder.encode("123456"));
        userMapper.insert(u);
        TeacherProfile tp = new TeacherProfile();
        tp.setUserId(u.getId()); tp.setTitle(title); tp.setIntro(intro);
        tp.setRating(BigDecimal.valueOf(5.0)); tp.setAuditStatus(0); // 待审核
        teacherMapper.insert(tp);
        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(u.getId()); ts.setSubjectId(s.getId());
        teacherSubjectMapper.insert(ts);
    }

    private Subject subj(String name, String cat) {
        Subject s = new Subject(); s.setName(name); s.setCategory(cat);
        subjectMapper.insert(s); return s;
    }

    private long teacher(String phone, String name, String title, String intro, double rating, Subject s) {
        User u = new User();
        u.setPhone(phone); u.setName(name); u.setRole(2); u.setStatus(1);
        u.setPasswordHash(encoder.encode("123456"));
        userMapper.insert(u);
        TeacherProfile tp = new TeacherProfile();
        tp.setUserId(u.getId()); tp.setTitle(title); tp.setIntro(intro);
        tp.setRating(BigDecimal.valueOf(rating)); tp.setAuditStatus(1);
        teacherMapper.insert(tp);
        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(u.getId()); ts.setSubjectId(s.getId());
        teacherSubjectMapper.insert(ts);
        return u.getId();
    }

    private void tpl(long teacherId, int weekday, LocalTime st, LocalTime et, Subject s) {
        TimeslotTemplate t = new TimeslotTemplate();
        t.setTeacherId(teacherId); t.setWeekday(weekday);
        t.setStartTime(st); t.setEndTime(et); t.setSubjectId(s.getId()); t.setEnabled(1);
        templateMapper.insert(t);
    }
}
