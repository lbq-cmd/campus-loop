package com.campusloop.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("同一密码两次加密结果不同（随机盐），但都能校验通过")
    void encodeUsesRandomSalt() {
        String first = encoder.encode("Abcd1234");
        String second = encoder.encode("Abcd1234");
        assertNotEquals(first, second);
        assertTrue(encoder.matches("Abcd1234", first));
        assertTrue(encoder.matches("Abcd1234", second));
    }

    @Test
    @DisplayName("错误密码校验失败")
    void wrongPasswordDoesNotMatch() {
        String hash = encoder.encode("Abcd1234");
        assertFalse(encoder.matches("abcd1234", hash));
        assertFalse(encoder.matches("Abcd12345", hash));
    }

    @Test
    @DisplayName("密文包含算法前缀与迭代次数，便于后续升级")
    void encodedFormatContainsMetadata() {
        String hash = encoder.encode("Abcd1234");
        String[] parts = hash.split("\\$");
        assertEquals(4, parts.length);
        assertEquals("pbkdf2", parts[0]);
        assertEquals(120_000, Integer.parseInt(parts[1]));
    }

    @Test
    @DisplayName("空密码或非法密文不应抛异常，按校验失败处理")
    void invalidInputHandledDefensively() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(""));
        assertFalse(encoder.matches(null, "pbkdf2$1$a$b"));
        assertFalse(encoder.matches("Abcd1234", "not-a-valid-hash"));
        assertFalse(encoder.matches("Abcd1234", "pbkdf2$120000$!!!$!!!"));
    }
}
