package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.TeacherProfile;
import com.bookmate.entity.TeacherSubject;
import com.bookmate.entity.User;
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

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, TeacherProfileMapper teacherMapper,
                       TeacherSubjectMapper teacherSubjectMapper, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.teacherMapper = teacherMapper;
        this.teacherSubjectMapper = teacherSubjectMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public Map<String, Object> login(String phone, String password, int role, Long subjectId, String name) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (u == null) {
            if (role == 2 && subjectId == null) {
                throw new IllegalArgumentException("老师注册需选择授课科目");
            }
            // 真实注册：手机号 + 密码 + 角色
            u = new User();
            u.setPhone(phone);
            u.setName((name == null || name.isBlank())
                    ? (role == 2 ? "老师" + phone.substring(Math.max(0, phone.length() - 4))
                                 : "学员" + phone.substring(Math.max(0, phone.length() - 4)))
                    : name);
            u.setRole(role);
            u.setStatus(1);
            u.setPasswordHash(encoder.encode(password));
            userMapper.insert(u);

            if (role == 2) {
                // 老师注册：创建待审核档案 + 科目关联（管理员审核通过后才对学员可见）
                TeacherProfile tp = new TeacherProfile();
                tp.setUserId(u.getId());
                tp.setTitle("新入驻老师");
                tp.setIntro("");
                tp.setRating(BigDecimal.valueOf(5.0));
                tp.setAuditStatus(0); // 待审核
                teacherMapper.insert(tp);

                TeacherSubject ts = new TeacherSubject();
                ts.setTeacherId(u.getId());
                ts.setSubjectId(subjectId);
                teacherSubjectMapper.insert(ts);
            }
        } else {
            if (u.getRole() != role) {
                throw new IllegalArgumentException("该账号的身份与所选不符，请选择正确的身份登录");
            }
            if (!encoder.matches(password, u.getPasswordHash())) {
                throw new IllegalArgumentException("密码错误");
            }
        }
        String token = jwtUtil.generate(u.getId(), String.valueOf(u.getRole()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("userId", u.getId());
        out.put("role", u.getRole());
        out.put("name", u.getName());
        // 老师附带审核状态，前端可提示
        if (u.getRole() == 2) {
            TeacherProfile tp = teacherMapper.selectOne(
                    new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, u.getId()));
            out.put("auditStatus", tp != null ? tp.getAuditStatus() : -1);
        }
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
}
