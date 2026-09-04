package com.bookmate.common;

import com.bookmate.util.JwtUtil;
import org.springframework.stereotype.Component;

/**
 * 鉴权接缝：Controller 只认本类，不再各自剥离 Bearer 前缀、不再直接摸 JwtUtil。
 * 原先 currentUserId() 在 4 个 Controller 复制（含一处死代码），收敛于此。
 */
@Component
public class AuthHelper {
    public static final int ROLE_STUDENT = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_ADMIN = 3;

    private final JwtUtil jwtUtil;

    public AuthHelper(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 从 Authorization 头解析当前用户 id（token 无效则抛异常，由全局兜底） */
    public long userId(String authHeader) {
        return jwtUtil.parseUserId(strip(authHeader));
    }

    /** 当前用户是否指定角色（token 无效一律 false） */
    public boolean hasRole(String authHeader, int role) {
        try {
            Integer r = jwtUtil.parseRole(strip(authHeader));
            return r != null && r == role;
        } catch (Exception e) {
            return false;
        }
    }

    private String strip(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
