package com.campusloop.common;

import com.campusloop.common.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库访问入口：负责连接的获取、外键开关与事务边界。
 *
 * <p>设计说明：
 * <ul>
 *   <li>单例模式：应用运行期只保留一个默认实例（{@link #getDefault()}），统一数据库地址；</li>
 *   <li>测试友好：同时允许 {@code new Database(url)} 构造独立实例，便于每个测试用独立临时库；</li>
 *   <li>事务模板：{@link #inTransaction(TxWork)} 统一处理提交、回滚与自动提交恢复，
 *       业务层无需重复编写 try/catch/finally，减少模板代码。</li>
 * </ul>
 */
public final class Database {

    private static volatile Database defaultInstance;

    private final String url;

    public Database(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("数据库连接地址不能为空");
        }
        this.url = url.trim();
    }

    /** 应用默认数据库（单例）。 */
    public static Database getDefault() {
        Database local = defaultInstance;
        if (local == null) {
            synchronized (Database.class) {
                local = defaultInstance;
                if (local == null) {
                    local = new Database(AppConfig.get().getDbUrl());
                    defaultInstance = local;
                }
            }
        }
        return local;
    }

    public String getUrl() {
        return url;
    }

    /**
     * 获取一条新连接。SQLite 需要每个连接单独打开外键约束。
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /** 事务内执行的数据库操作。 */
    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /**
     * 在一个事务中执行给定操作：正常返回则提交，任何异常则回滚。
     */
    public <T> T inTransaction(TxWork<T> work) {
        if (work == null) {
            throw new IllegalArgumentException("事务操作不能为空");
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new DataAccessException("事务执行失败：" + e.getMessage(), e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // 连接即将关闭，恢复自动提交失败无需中断主流程
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("获取数据库连接失败：" + e.getMessage(), e);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 回滚失败通常意味着连接已断开，交由上层关闭连接处理
        }
    }
}
