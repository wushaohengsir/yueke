package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.StudentProfile;
import com.bookmate.entity.TeacherProfile;
import com.bookmate.entity.TeacherSubject;
import com.bookmate.entity.User;
import com.bookmate.mapper.StudentProfileMapper;
import com.bookmate.mapper.TeacherProfileMapper;
import com.bookmate.mapper.TeacherSubjectMapper;
import com.bookmate.mapper.UserMapper;
import com.bookmate.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 注册 / 登录。注册与登录已分离：
 *  - register：公开注册，仅限学员(1)/老师(2)。学员直接建档并可登录；老师建档后 audit_status=0 待审，
 *    审核通过(1)前无法登录老师端（login 门禁拦截）。
 *  - login：纯登录，不做「查无此号自动建档」。未注册/被禁用/待审老师都会给出明确错误。
 */
@Service
public class AuthService {
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, TeacherProfileMapper teacherMapper,
                       TeacherSubjectMapper teacherSubjectMapper, StudentProfileMapper studentProfileMapper,
                       PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.teacherMapper = teacherMapper;
        this.teacherSubjectMapper = teacherSubjectMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    /** 登录（纯登录）。未通过审核/被驳回的老师在此被拒，拿不到 token。 */
    public Map<String, Object> login(String phone, String password, int role) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (u == null) {
            throw new IllegalArgumentException("该手机号尚未注册，请先注册");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new IllegalArgumentException("该账号已被禁用，请联系管理员");
        }
        if (u.getRole() != role) {
            throw new IllegalArgumentException("该账号的身份与所选不符，请选择正确的身份登录");
        }
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 老师登录门禁：审核通过前不得进入老师端（注册后需管理员审核，「通过」才算完成注册）
        if (role == 2) {
            TeacherProfile tp = profileOf(u.getId());
            if (tp == null) {
                throw new IllegalArgumentException("老师档案异常，请联系管理员");
            }
            if (tp.getAuditStatus() == 0) {
                throw new IllegalArgumentException("您的入驻申请正在审核中，通过后即可登录老师端");
            }
            if (tp.getAuditStatus() == 2) {
                throw new IllegalArgumentException("您的入驻申请未通过审核，请联系管理员处理");
            }
        }

        String token = jwtUtil.generate(u.getId(), String.valueOf(u.getRole()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("userId", u.getId());
        out.put("role", u.getRole());
        out.put("name", u.getName());
        if (role == 2) out.put("auditStatus", profileOf(u.getId()).getAuditStatus()); // 通过时恒为 1
        return out;
    }

    /**
     * 公开注册：role 仅限 1(学员)/2(老师)。
     *  - 学员：直接创建账号 + student_profile，返回 token（自动登录）。
     *  - 老师：创建账号 + 待审档案(audit_status=0) + 科目关联，不返回 token，等待管理员审核。
     */
    @Transactional
    public Map<String, Object> register(int role, String name, String phone, String password, Long subjectId) {
        if (role != 1 && role != 2) {
            throw new IllegalArgumentException("仅支持注册学员或老师账号");
        }
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("请输入手机号");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("密码至少 6 位");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("请填写姓名");
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该手机号已注册，请直接登录");
        }

        User u = new User();
        u.setPhone(phone);
        u.setName(name.trim());
        u.setRole(role);
        u.setStatus(1);
        u.setPasswordHash(encoder.encode(password));
        userMapper.insert(u);

        if (role == 1) {
            // 学员建档（booking/课时表均外键到 student_profile，缺档将无法约课）
            StudentProfile sp = new StudentProfile();
            sp.setUserId(u.getId());
            sp.setCreditsTotal(0);
            sp.setCreditsUsed(0);
            studentProfileMapper.insert(sp);

            String token = jwtUtil.generate(u.getId(), String.valueOf(role));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("token", token);
            out.put("userId", u.getId());
            out.put("role", role);
            out.put("name", u.getName());
            return out;
        }

        // 老师：待审核档案 + 科目关联（审核通过(audit_status=1)后学员可见、可登录）
        if (subjectId == null) throw new IllegalArgumentException("老师注册需选择授课科目");
        TeacherProfile tp = new TeacherProfile();
        tp.setUserId(u.getId());
        tp.setTitle("新入驻老师");
        tp.setIntro("");
        tp.setRating(BigDecimal.valueOf(5.0));
        tp.setAuditStatus(0);
        teacherMapper.insert(tp);

        TeacherSubject ts = new TeacherSubject();
        ts.setTeacherId(u.getId());
        ts.setSubjectId(subjectId);
        teacherSubjectMapper.insert(ts);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", u.getId());
        out.put("role", role);
        out.put("name", u.getName());
        out.put("auditStatus", 0); // 待审核；无 token，登录需等审核通过
        return out;
    }

    // 当前登录用户资料（前端「我的」页按 token 刷新，取后端真实姓名/手机号）
    public Map<String, Object> me(long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", u.getId());
        out.put("role", u.getRole());
        out.put("name", u.getName());
        out.put("phone", u.getPhone());
        return out;
    }

    private TeacherProfile profileOf(long teacherUserId) {
        return teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, teacherUserId));
    }
}
