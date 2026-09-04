package com.bookmate.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expire-hours}") private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(long userId, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireHours * 3600_000))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parseUserId(String token) {
        var claims = Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }

    // 解析角色（登录时写入的 role claim，数字字符串：1学员2老师3管理员）
    public Integer parseRole(String token) {
        var claims = Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
        Object role = claims.get("role");
        return role == null ? null : Integer.valueOf(String.valueOf(role));
    }
}
