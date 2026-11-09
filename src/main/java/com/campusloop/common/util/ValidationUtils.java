package com.campusloop.common.util;

import com.campusloop.common.exception.ValidationException;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 通用输入校验工具：所有对外入口（服务层）统一调用，保证非法输入在进入数据库前被拦截。
 */
public final class ValidationUtils {

    /** 用户名：4-20 位字母、数字或下划线，必须以字母开头。 */
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");

    private ValidationUtils() {
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new ValidationException(field, field + "不能为空");
        }
        return value;
    }

    public static String requireText(String value, String field, int minLength, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field, field + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() < minLength) {
            throw new ValidationException(field, field + "长度不能少于 " + minLength + " 个字符");
        }
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field, field + "长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field, field + "长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    public static String requireUsername(String username) {
        if (username == null || !USERNAME.matcher(username.trim()).matches()) {
            throw new ValidationException("username", "用户名需为 4-20 位字母、数字或下划线，且以字母开头");
        }
        return username.trim();
    }

    /**
     * 密码强度：8-32 位，至少包含字母与数字。
     */
    public static void requireStrongPassword(String password, String field) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            throw new ValidationException(field, "密码长度需为 8-32 位");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException(field, "密码必须同时包含字母和数字");
        }
    }

    public static double requireRange(double value, String field, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ValidationException(field, field + "必须是有限数值");
        }
        if (value < min || value > max) {
            throw new ValidationException(field, field + "必须在 " + min + " 到 " + max + " 之间");
        }
        return value;
    }

    public static long requirePositiveId(long id, String field) {
        if (id <= 0) {
            throw new ValidationException(field, field + "必须为正整数");
        }
        return id;
    }

    public static <T extends Collection<?>> T requireMaxSize(T collection, String field, int maxSize) {
        if (collection != null && collection.size() > maxSize) {
            throw new ValidationException(field, field + "数量不能超过 " + maxSize);
        }
        return collection;
    }
}
