package com.campusloop.dao.jdbc;

import com.campusloop.common.Database;
import com.campusloop.common.exception.DataAccessException;
import com.campusloop.common.util.Times;
import com.campusloop.dao.UserDao;
import com.campusloop.domain.Role;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户表 JDBC 实现：全部使用预编译语句，杜绝 SQL 注入；受检异常统一转换为 DataAccessException。
 */
public final class JdbcUserDao implements UserDao {

    private static final String COLUMNS =
            "id, username, password_hash, nickname, contact, role, status, credit_score, created_at, last_login_at";

    private final Database database;

    public JdbcUserDao(Database database) {
        this.database = database;
    }

    @Override
    public long insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, nickname, contact, role, status, credit_score, created_at, last_login_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getNickname());
            setNullableString(statement, 4, user.getContact());
            statement.setString(5, user.getRole().name());
            statement.setString(6, user.getStatus().name());
            statement.setInt(7, user.getCreditScore());
            statement.setString(8, Times.toText(user.getCreatedAt()));
            statement.setString(9, Times.toText(user.getLastLoginAt()));
            statement.executeUpdate();
            return readGeneratedKey(connection);
        } catch (SQLException e) {
            throw new DataAccessException("新增用户失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("更新用户时 id 不能为空");
        }
        String sql = "UPDATE users SET password_hash = ?, nickname = ?, contact = ?, role = ?,"
                + " status = ?, credit_score = ?, last_login_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getPasswordHash());
            statement.setString(2, user.getNickname());
            setNullableString(statement, 3, user.getContact());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getStatus().name());
            statement.setInt(6, user.getCreditScore());
            statement.setString(7, Times.toText(user.getLastLoginAt()));
            statement.setLong(8, user.getId());
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新用户失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne("SELECT " + COLUMNS + " FROM users WHERE username = ?", username);
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) {
            return false;
        }
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询用户名是否存在失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<User> search(String keyword, int limit) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String sql = "SELECT " + COLUMNS + " FROM users"
                + (hasKeyword ? " WHERE username LIKE ? ESCAPE '\\' OR nickname LIKE ? ESCAPE '\\'" : "")
                + " ORDER BY id ASC LIMIT ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (hasKeyword) {
                String pattern = "%" + escapeLike(keyword.trim()) + "%";
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
            }
            statement.setInt(index, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询用户列表失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void touchLastLogin(long userId, LocalDateTime time) {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Times.toText(time));
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新登录时间失败：" + e.getMessage(), e);
        }
    }

    private Optional<User> findOne(String sql, Object parameter) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter instanceof Long) {
                statement.setLong(1, (Long) parameter);
            } else {
                statement.setString(1, String.valueOf(parameter));
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询用户失败：" + e.getMessage(), e);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("contact"),
                Role.valueOf(rs.getString("role")),
                UserStatus.valueOf(rs.getString("status")),
                rs.getInt("credit_score"),
                Times.parse(rs.getString("created_at")),
                Times.parse(rs.getString("last_login_at")));
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 读取当前连接最后一次插入的自增主键。
     *
     * <p>SQLite 驱动未实现 {@code getGeneratedKeys()}，官方推荐在同一连接上执行
     * {@code SELECT last_insert_rowid()}；由于连接不共享，该值必然是本次插入产生的主键。
     */
    static long readGeneratedKey(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet keys = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new DataAccessException("插入成功但未能获取自增主键");
    }
}
