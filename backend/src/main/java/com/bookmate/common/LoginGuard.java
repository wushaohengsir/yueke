package com.bookmate.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防爆破：按手机号累计连续失败，超过阈值临时锁定。
 * 内存级、单实例足够（当前单机部署）；多实例时需换 Redis 计数。
 */
@Component
public class LoginGuard {
    static final int MAX_FAILS = 5;
    static final long WINDOW_MS = 10 * 60_000L; // 计数窗口 / 锁定时长

    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    private static class Entry {
        int fails;
        long windowStart;
        long lockUntil;
    }

    /** 仍被锁定的剩余分钟数；未锁定返回 0 */
    public int lockRemainingMinutes(String key) {
        Entry e = map.get(key);
        if (e == null) return 0;
        long n = System.currentTimeMillis();
        if (e.lockUntil > n) {
            return (int) Math.max(1, Math.ceil((e.lockUntil - n) / 60000.0));
        }
        return 0;
    }

    /** 记一次失败；达到阈值即锁定窗口时长 */
    public void recordFailure(String key) {
        long n = System.currentTimeMillis();
        Entry e = map.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (e.lockUntil > n) return; // 已锁定，不再累计
            if (n - e.windowStart > WINDOW_MS) { // 上一窗口已过期，重新计数
                e.fails = 0;
                e.windowStart = n;
            }
            e.fails++;
            if (e.fails >= MAX_FAILS) e.lockUntil = n + WINDOW_MS;
        }
    }

    /** 登录成功清除计数 */
    public void reset(String key) {
        map.remove(key);
    }
}
