package com.bookmate.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginGuardTest {

    @Test
    void 连续失败达阈值锁定_成功登录复位() {
        LoginGuard g = new LoginGuard();
        String key = "13900000000";
        assertEquals(0, g.lockRemainingMinutes(key));
        for (int i = 0; i < LoginGuard.MAX_FAILS; i++) {
            g.recordFailure(key);
        }
        assertTrue(g.lockRemainingMinutes(key) > 0, "5 次失败后应进入锁定期");
        // 锁定后再失败不再累计（不报错）
        g.recordFailure(key);
        assertTrue(g.lockRemainingMinutes(key) > 0);
        // 登录成功复位
        g.reset(key);
        assertEquals(0, g.lockRemainingMinutes(key));
    }

    @Test
    void 未达阈值不锁定() {
        LoginGuard g = new LoginGuard();
        g.recordFailure("13800000000");
        g.recordFailure("13800000000");
        assertEquals(0, g.lockRemainingMinutes("13800000000"));
    }
}
