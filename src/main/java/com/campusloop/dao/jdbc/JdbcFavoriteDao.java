package com.campusloop.dao.jdbc;

import com.campusloop.common.Database;
import com.campusloop.common.exception.DataAccessException;
import com.campusloop.common.util.Times;
import com.campusloop.dao.FavoriteDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 收藏表 JDBC 实现：依靠复合主键 + INSERT OR IGNORE 实现幂等收藏。
 */
public final class JdbcFavoriteDao implements FavoriteDao {

    private final Database database;

    public JdbcFavoriteDao(Database database) {
        this.database = database;
    }

    @Override
    public boolean add(long userId, long itemId) {
        String sql = "INSERT OR IGNORE INTO favorites (user_id, item_id, created_at) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, itemId);
            statement.setString(3, Times.toText(LocalDateTime.now()));
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("收藏失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean remove(long userId, long itemId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND item_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, itemId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("取消收藏失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(long userId, long itemId) {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND item_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, itemId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询收藏状态失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<Long> findItemIdsByUser(long userId) {
        String sql = "SELECT item_id FROM favorites WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
                return ids;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询收藏列表失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int countByItem(long itemId) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE item_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计收藏数失败：" + e.getMessage(), e);
        }
    }
}
