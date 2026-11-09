package com.campusloop.common.util;

import com.campusloop.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationUtilsTest {

    @Test
    @DisplayName("合法用户名通过校验并去除首尾空格")
    void validUsernamePassed() {
        assertEquals("zhang_san1", ValidationUtils.requireUsername("  zhang_san1 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1abc", "a b c", "张三", "abcdefghijklmnopqrstu", "a-b-c"})
    @DisplayName("非法用户名被拒绝")
    void invalidUsernameRejected(String username) {
        assertThrows(ValidationException.class, () -> ValidationUtils.requireUsername(username));
    }

    @Test
    @DisplayName("密码必须同时包含字母和数字")
    void passwordStrengthChecked() {
        assertThrows(ValidationException.class, () -> ValidationUtils.requireStrongPassword("12345678", "密码"));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireStrongPassword("abcdefgh", "密码"));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireStrongPassword("Ab1", "密码"));
        ValidationUtils.requireStrongPassword("Abcd1234", "密码");
    }

    @Test
    @DisplayName("文本长度校验：去空格、判空、判上下界")
    void textLengthChecked() {
        assertEquals("台灯", ValidationUtils.requireText("  台灯  ", "标题", 2, 60));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireText("   ", "标题", 2, 60));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireText("灯", "标题", 2, 60));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireText("灯".repeat(61), "标题", 2, 60));
    }

    @Test
    @DisplayName("可选文本：空白转为 null")
    void optionalTextNormalized() {
        assertNull(ValidationUtils.optionalText("   ", "描述", 10));
        assertNull(ValidationUtils.optionalText(null, "描述", 10));
        assertEquals("备注", ValidationUtils.optionalText(" 备注 ", "描述", 10));
        assertThrows(ValidationException.class, () -> ValidationUtils.optionalText("超长超长超长", "描述", 4));
    }

    @Test
    @DisplayName("数值区间校验包含边界，拒绝 NaN/无穷")
    void rangeChecked() {
        assertEquals(0, ValidationUtils.requireRange(0, "价格", 0, 100));
        assertEquals(100, ValidationUtils.requireRange(100, "价格", 0, 100));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireRange(-1, "价格", 0, 100));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireRange(101, "价格", 0, 100));
        assertThrows(ValidationException.class, () -> ValidationUtils.requireRange(Double.NaN, "价格", 0, 100));
        assertThrows(ValidationException.class,
                () -> ValidationUtils.requireRange(Double.POSITIVE_INFINITY, "价格", 0, 100));
    }

    @Test
    @DisplayName("ID 必须为正整数")
    void idMustBePositive() {
        assertEquals(3, ValidationUtils.requirePositiveId(3, "用户ID"));
        assertThrows(ValidationException.class, () -> ValidationUtils.requirePositiveId(0, "用户ID"));
        assertThrows(ValidationException.class, () -> ValidationUtils.requirePositiveId(-5, "用户ID"));
    }

    @Test
    @DisplayName("集合大小上限校验")
    void collectionSizeChecked() {
        ValidationUtils.requireMaxSize(Arrays.asList("a", "b"), "标签", 2);
        assertThrows(ValidationException.class,
                () -> ValidationUtils.requireMaxSize(Arrays.asList("a", "b", "c"), "标签", 2));
    }
}
