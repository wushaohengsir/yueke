package com.bookmate.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.User;
import com.bookmate.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员引导：仅当系统中不存在管理员时创建初始管理员账号（审核入口必需）。
 * 不注入任何业务演示数据——老师/学员/科目/时段均走真实注册与创建流程。
 */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;

    public AdminBootstrap(UserMapper u, PasswordEncoder e) {
        this.userMapper = u;
        this.encoder = e;
    }

    @Override
    public void run(String... args) {
        Long admins = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, 3));
        if (admins != null && admins > 0) return;
        User admin = new User();
        admin.setPhone("13900000000"); admin.setName("管理员");
        admin.setRole(3); admin.setStatus(1);
        admin.setPasswordHash(encoder.encode("123456"));
        userMapper.insert(admin);
        System.out.println("[AdminBootstrap] created initial admin 13900000000/123456 (change in production)");
    }
}
