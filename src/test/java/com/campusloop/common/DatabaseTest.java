package com.campusloop.common;

import com.campusloop.common.exception.DataAccessException;
import com.campusloop.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    private TestDb testDb;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    @Test
    @DisplayName("事务正常结束自动提交")
    void transactionCommits() {
        Database database = testDb.database();
        database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash, nickname, role, status, credit_score, created_at)"
                            + " VALUES ('tx_commit', 'hash', '测试', 'STUDENT', 'ACTIVE', 100, '2026-11-08T12:00:00')")) {
                ps.executeUpdate();
            }
            return null;
        });
        assertTrue(testDb.userDao().existsByUsername("tx_commit"));
    }

    @Test
    @DisplayName("事务内出现异常时整体回滚")
    void transactionRollsBack() {
        Database database = testDb.database();
        assertThrows(DataAccessException.class, () -> database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash, nickname, role, status, credit_score, created_at)"
                            + " VALUES ('tx_rollback', 'hash', '测试', 'STUDENT', 'ACTIVE', 100, '2026-11-08T12:00:00')")) {
                ps.executeUpdate();
            }
            // 外键指向不存在的分类，触发 SQLException 使事务回滚
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO items (owner_id, category_id, title, item_type, price, item_condition, status,"
                            + " view_count, created_at, updated_at)"
                            + " VALUES (1, 999999, '非法物品', 'SELL', 10, 'GOOD', 'ONLINE', 0,"
                            + " '2026-11-08T12:00:00', '2026-11-08T12:00:00')")) {
                ps.executeUpdate();
            }
            return null;
        }));
        assertFalse(testDb.userDao().existsByUsername("tx_rollback"));
    }

    @Test
    @DisplayName("业务异常同样触发回滚，并原样抛出")
    void businessExceptionRollsBack() {
        Database database = testDb.database();
        assertThrows(IllegalStateException.class, () -> database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash, nickname, role, status, credit_score, created_at)"
                            + " VALUES ('tx_business', 'hash', '测试', 'STUDENT', 'ACTIVE', 100, '2026-11-08T12:00:00')")) {
                ps.executeUpdate();
            }
            throw new IllegalStateException("业务校验失败");
        }));
        assertFalse(testDb.userDao().existsByUsername("tx_business"));
    }

    @Test
    @DisplayName("连接打开外键约束")
    void foreignKeysEnabled() throws SQLException {
        try (var connection = testDb.database().getConnection();
             var statement = connection.createStatement();
             var rs = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) == 1);
        }
    }

    @Test
    @DisplayName("空连接地址被拒绝")
    void blankUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Database("  "));
    }
}
