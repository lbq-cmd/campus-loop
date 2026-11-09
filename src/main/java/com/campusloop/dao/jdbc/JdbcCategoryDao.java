package com.campusloop.dao.jdbc;

import com.campusloop.common.Database;
import com.campusloop.common.exception.DataAccessException;
import com.campusloop.dao.CategoryDao;
import com.campusloop.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 分类表 JDBC 实现。
 */
public final class JdbcCategoryDao implements CategoryDao {

    private final Database database;

    public JdbcCategoryDao(Database database) {
        this.database = database;
    }

    @Override
    public long insert(Category category) {
        String sql = "INSERT INTO categories (name, description, sort_order) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            if (category.getDescription() == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, category.getDescription());
            }
            statement.setInt(3, category.getSortOrder());
            statement.executeUpdate();
            return JdbcUserDao.readGeneratedKey(connection);
        } catch (SQLException e) {
            throw new DataAccessException("新增分类失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        return findOne("SELECT id, name, description, sort_order FROM categories WHERE id = ?", id);
    }

    @Override
    public Optional<Category> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return findOne("SELECT id, name, description, sort_order FROM categories WHERE name = ?", name);
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name, description, sort_order FROM categories ORDER BY sort_order ASC, id ASC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("查询分类失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    private Optional<Category> findOne(String sql, Object parameter) {
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
            throw new DataAccessException("查询分类失败：" + e.getMessage(), e);
        }
    }

    private static Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getInt("sort_order"));
    }
}
