package com.bookmate.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 全站时间口径：中国时区。原先 ZoneId.of("Asia/Shanghai") 散落在各 Service/Controller，
 * 收敛到此处，改一处全站生效。
 */
public final class AppTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private AppTime() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
