package com.bookmate.service;

import com.bookmate.entity.User;
import com.bookmate.mapper.UserMapper;
import com.bookmate.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色越权 / 公网裸奔防护：管理端、老师端、学员端按 URL 前缀集中收口，
 * 任意登录者（如学员）不得触达管理接口；/api/auth/me 也须带有效登录态。
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserMapper userMapper;

    private String bearer(long userId, int role) {
        return "Bearer " + jwtUtil.generate(userId, String.valueOf(role));
    }

    // ---- 越权：学员/老师 token 一律不得进管理端 ----

    @Test
    void 学员token访问管理端用户列表应403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(2L, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 老师token访问管理端看板应403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(1L, 2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 管理员token访问管理端用户列表应200() throws Exception {
        // token 角色为管理员，且 userId 指向真实启用中的用户(seeded id=1) → 通过 JWT 即时校验
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(1L, 3)))
                .andExpect(status().isOk());
    }

    // ---- JWT 即时吊销：被禁用账号即使持有 token 也立即失效力 ----

    @Test
    void 被禁用账号token即时失效应403() throws Exception {
        User u = userMapper.selectById(2L);
        u.setStatus(0);
        userMapper.updateById(u);
        mockMvc.perform(get("/api/credits")
                        .header("Authorization", bearer(2L, 1)))
                .andExpect(status().isForbidden());
    }

    // ---- 角色错位：学员 token 不得进老师端 ----

    @Test
    void 学员token访问老师端周课表应403() throws Exception {
        mockMvc.perform(get("/api/teacher/week-schedule")
                        .header("Authorization", bearer(2L, 1)))
                .andExpect(status().isForbidden());
    }

    // ---- /api/auth/me 需登录态（避免无 token 裸奔到用户资料口）；匿名被 Spring 拒(403) ----

    @Test
    void 无token访问me应403() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isForbidden());
    }

    // ---- 公开口仍可用：老师名录/登录 ----

    @Test
    void 游客可读老师名录() throws Exception {
        mockMvc.perform(get("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
