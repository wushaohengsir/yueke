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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private UserMapper userMapper;
    @Autowired private TeacherProfileMapper teacherMapper;
    @Autowired private TeacherSubjectMapper teacherSubjectMapper;
    @Autowired private StudentProfileMapper studentProfileMapper;

    private User byPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    // ---- 注册 ----

    @Test
    void register_学员建档并自动登录返回token() {
        Map<String, Object> r = authService.register(1, "林小雨", "13911110001", "123456", null);
        assertTrue(String.valueOf(r.get("token")).length() > 10);
        assertEquals(1, ((Number) r.get("role")).intValue());

        User u = byPhone("13911110001");
        assertNotNull(u);
        assertEquals("林小雨", u.getName());
        assertEquals(1, u.getRole());
        // 学员档案必须存在（约课/课时外键依赖）
        Long sp = studentProfileMapper.selectCount(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, u.getId()));
        assertEquals(1L, sp);
    }

    @Test
    void register_老师创建待审档案且无token() {
        Map<String, Object> r = authService.register(2, "周老师", "13911110002", "123456", 1L);
        assertNull(r.get("token"));
        assertEquals(0, ((Number) r.get("auditStatus")).intValue());

        User u = byPhone("13911110002");
        assertNotNull(u);
        TeacherProfile tp = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, u.getId()));
        assertNotNull(tp);
        assertEquals(0, tp.getAuditStatus()); // 待审核
        Long ts = teacherSubjectMapper.selectCount(new LambdaQueryWrapper<TeacherSubject>()
                .eq(TeacherSubject::getTeacherId, u.getId()));
        assertEquals(1L, ts);
    }

    @Test
    void register_老师缺科目拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(2, "周老师", "13911110003", "123456", null));
    }

    @Test
    void register_重复手机号拒绝() {
        authService.register(1, "林小雨", "13911110004", "123456", null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.register(2, "周老师", "13911110004", "123456", 1L));
        assertTrue(e.getMessage().contains("已注册"));
    }

    @Test
    void register_非法角色管理员拒绝() {
        // 公开注册不允许自封管理员（管理员由启动引导或在任管理员创建）
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(3, "管理员", "13911110005", "123456", null));
    }

    // ---- 登录门禁 ----

    @Test
    void login_未注册手机号拒绝() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.login("13999990000", "123456", 1));
        assertTrue(e.getMessage().contains("尚未注册"));
    }

    @Test
    void login_老师待审拒绝() {
        authService.register(2, "周老师", "13911110006", "123456", 1L);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.login("13911110006", "123456", 2));
        assertTrue(e.getMessage().contains("审核中"));
    }

    @Test
    void login_老师通过后放行() {
        authService.register(2, "周老师", "13911110007", "123456", 1L);
        User u = byPhone("13911110007");
        TeacherProfile tp = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, u.getId()));
        tp.setAuditStatus(1);
        teacherMapper.updateById(tp);

        Map<String, Object> r = authService.login("13911110007", "123456", 2);
        assertNotNull(r.get("token"));
        assertEquals(1, ((Number) r.get("auditStatus")).intValue());
    }

    @Test
    void login_老师驳回拒绝() {
        authService.register(2, "周老师", "13911110008", "123456", 1L);
        User u = byPhone("13911110008");
        TeacherProfile tp = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, u.getId()));
        tp.setAuditStatus(2);
        teacherMapper.updateById(tp);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.login("13911110008", "123456", 2));
        assertTrue(e.getMessage().contains("未通过"));
    }

    @Test
    void login_学员正常返回token() {
        authService.register(1, "林小雨", "13911110009", "123456", null);
        Map<String, Object> r = authService.login("13911110009", "123456", 1);
        assertNotNull(r.get("token"));
        assertEquals("林小雨", r.get("name"));
    }

    @Test
    void login_密码错误拒绝() {
        authService.register(1, "林小雨", "13911110010", "123456", null);
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("13911110010", "wrong1", 1));
    }

    @Test
    void login_身份与所选不符拒绝() {
        authService.register(1, "林小雨", "13911110011", "123456", null);
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("13911110011", "123456", 2));
    }

    @Test
    void login_被禁用账号拒绝() {
        authService.register(1, "林小雨", "13911110012", "123456", null);
        User u = byPhone("13911110012");
        u.setStatus(0);
        userMapper.updateById(u);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.login("13911110012", "123456", 1));
        assertTrue(e.getMessage().contains("禁用"));
    }
}
