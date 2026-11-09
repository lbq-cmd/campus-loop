package com.campusloop.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 时间工具：SQLite 没有原生日期类型，统一以 ISO-8601 字符串存储，保证排序与跨库迁移友好。
 */
public final class Times {

    private Times() {
    }

    public static String toText(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("非法的时间格式：" + text, e);
        }
    }
}
