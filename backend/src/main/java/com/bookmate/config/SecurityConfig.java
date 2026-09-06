package com.bookmate.config;

import com.bookmate.entity.User;
import com.bookmate.mapper.UserMapper;
import com.bookmate.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        return new JwtAuthFilter(jwtUtil, userMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter filter) throws Exception {
        http.csrf(c -> c.disable())
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // 仅公开：登录/注册/科目下拉/老师名录（游客浏览师资）
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/subjects").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/teachers").permitAll()
                // 角色隔离：按 URL 前缀强制角色（越权/裸奔的根因在此收口）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/bookings", "/api/credits", "/api/leave", "/api/contracts", "/api/contracts/**").hasRole("STUDENT")
                .anyRequest().authenticated())
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 收紧跨域：仅本站(nginx 同源)与本地开发源；其余跨站请求一律不带 CORS 放行头。
        // 同源访问（浏览器同 origin 调 /api）不依赖 CORS，故换 IP/域名访问不受影响。
        cfg.setAllowedOriginPatterns(List.of(
                "http://146.56.247.172",
                "http://localhost:5173",
                "http://127.0.0.1:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    static class JwtAuthFilter extends OncePerRequestFilter {
        private final JwtUtil jwtUtil;
        private final UserMapper userMapper;
        JwtAuthFilter(JwtUtil j, UserMapper userMapper) { this.jwtUtil = j; this.userMapper = userMapper; }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    Long uid = jwtUtil.parseUserId(auth.substring(7));
                    // 即时吊销：每个请求核对用户仍存在且未被禁用(status=1)，被停用/删除立即失效
                    User u = userMapper.selectById(uid);
                    if (u == null || u.getStatus() == null || u.getStatus().intValue() != 1) {
                        chain.doFilter(req, res);
                        return;
                    }
                    // token 里的角色号(1学员/2老师/3管理员)映射为 Spring 角色权限，
                    // 供 SecurityFilterChain 按 /api/** 前缀做集中式角色拦截。
                    Integer role = jwtUtil.parseRole(auth.substring(7));
                    String authority = "ROLE_USER";
                    if (role != null) {
                        authority = switch (role) {
                            case 1 -> "ROLE_STUDENT";
                            case 2 -> "ROLE_TEACHER";
                            case 3 -> "ROLE_ADMIN";
                            default -> "ROLE_USER";
                        };
                    }
                    var token = new UsernamePasswordAuthenticationToken(
                            uid, null, List.of(new SimpleGrantedAuthority(authority)));
                    SecurityContextHolder.getContext().setAuthentication(token);
                } catch (Exception ignored) { }
            }
            chain.doFilter(req, res);
        }
    }
}
