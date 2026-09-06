package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.StudentProfile;
import com.bookmate.entity.TeacherProfile;
import com.bookmate.entity.User;
import com.bookmate.mapper.StudentProfileMapper;
import com.bookmate.mapper.TeacherProfileMapper;
import com.bookmate.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminServiceIntegrationTest {

    @Autowired private AdminService adminService;
    @Autowired private AuthService authService;
    @Autowired private UserMapper userMapper;
    @Autowired private TeacherProfileMapper teacherMapper;
    @Autowired private StudentProfileMapper studentProfileMapper;

    private User byPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    // ---- 新建账号（学员/管理员）----

    @Test
    void createUser_新建管理员() {
        assertTrue(adminService.createUser(3, "管理员B", "13700000001", "admin123"));
        User u = byPhone("13700000001");
        assertNotNull(u);
        assertEquals(3, u.getRole());
        assertEquals("管理员B", u.getName());
        assertEquals(1, u.getStatus());
    }

    @Test
    void createUser_新建学员并建档() {
        assertTrue(adminService.createUser(1, "新学员", "13700000002", "stu1234"));
        User u = byPhone("13700000002");
        assertNotNull(u);
        assertEquals(1, u.getRole());
        Long sp = studentProfileMapper.selectCount(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, u.getId()));
        assertEquals(1L, sp);
    }

    @Test
    void createUser_重复手机号返回false() {
        assertTrue(adminService.createUser(1, "新学员", "13700000003", "stu1234"));
        assertFalse(adminService.createUser(3, "管理员B", "13700000003", "admin123"));
    }

    // ---- 管理员重置学员/老师密码（忘记密码场景）----

    @Test
    void resetPassword_学员重置后可登录_旧密码失效() {
        assertTrue(adminService.createUser(1, "忘记密码学员", "13711112222", "123456"));
        User u = byPhone("13711112222");
        assertNotNull(u);
        assertTrue(adminService.resetPassword(u.getId(), "abcdef")); // 重置新密码
        assertNotNull(authService.login("13711112222", "abcdef", 1).get("token")); // 新密码可登
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("13711112222", "123456", 1)); // 旧密码失效
    }

    @Test
    void resetPassword_管理员账号拒绝重置() {
        // test-data 无管理员；造一个 role=3 用户验证不可被重置（防管理员互越权）
        assertTrue(adminService.createUser(3, "管理员B", "13711113333", "admin123"));
        User admin = byPhone("13711113333");
        assertFalse(adminService.resetPassword(admin.getId(), "hacked123"));
    }

    // ---- 审核可改回（已驳回可再通过、已通过可再驳回）----

    @Test
    void auditTeacher_待审通过() {
        // test-data 老师 id=1 初始 audit_status=1（已通过）；先驳回再通过验证可改回
        assertTrue(adminService.auditTeacher(1L, false));  // 1 -> 2
        assertEquals(2, teacherMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, 1L)).getAuditStatus());
        assertTrue(adminService.auditTeacher(1L, true));   // 2 -> 1 驳回后可纠正为通过
        assertEquals(1, teacherMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getUserId, 1L)).getAuditStatus());
        assertFalse(adminService.auditTeacher(1L, true));  // 已是通过，再点通过不生效
    }

    @Test
    void auditTeacher_不存在老师返回false() {
        assertFalse(adminService.auditTeacher(999L, true));
    }
}
