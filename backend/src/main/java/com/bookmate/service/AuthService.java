package com.bookmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.User;
import com.bookmate.mapper.UserMapper;
import com.bookmate.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> login(String phone, String password, int role) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (u == null) {
            // 演示：自动注册
            u = new User();
            u.setPhone(phone);
            u.setName(role == 2 ? "老师" : "学员" + phone.substring(Math.max(0, phone.length() - 4)));
            u.setRole(role);
            u.setStatus(1);
            u.setPasswordHash(encoder.encode(password));
            userMapper.insert(u);
        } else if (!encoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }
        String token = jwtUtil.generate(u.getId(), String.valueOf(u.getRole()));
        return Map.of("token", token, "userId", u.getId(), "role", u.getRole(), "name", u.getName());
    }
}
