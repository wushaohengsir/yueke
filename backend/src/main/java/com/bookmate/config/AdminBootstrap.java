package com.bookmate.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmate.entity.User;
import com.bookmate.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 管理员引导：仅当系统中不存在管理员时创建初始管理员账号（审核入口必需）。
 * 不注入任何业务演示数据——老师/学员/科目/时段均走真实注册与创建流程。
 * 密码不再写死默认值：优先取环境变量 BOOKMATE_ADMIN_PASSWORD，否则随机生成并打印到日志（防"默认口令裸奔"）。
 */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final char[] ALNUM = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
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
        String pwd = System.getenv("BOOKMATE_ADMIN_PASSWORD");
        if (pwd == null || pwd.length() < 8) pwd = random(16);
        User admin = new User();
        admin.setPhone("13900000000"); admin.setName("管理员");
        admin.setRole(3); admin.setStatus(1);
        admin.setPasswordHash(encoder.encode(pwd));
        userMapper.insert(admin);
        System.out.println("[AdminBootstrap] created initial admin 13900000000 / " + pwd
                + "  (set env BOOKMATE_ADMIN_PASSWORD to override)");
    }

    private static String random(int n) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ALNUM[r.nextInt(ALNUM.length)]);
        return sb.toString();
    }
}
