package com.campusloop.dao.jdbc;

import com.campusloop.common.Database;
import com.campusloop.common.exception.DataAccessException;
import com.campusloop.common.util.Times;
import com.campusloop.dao.ItemDao;
import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.SearchCriteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 物品表 JDBC 实现。
 *
 * <p>检索采用「动态条件 + 预编译参数」：只拼接固定的 SQL 片段，用户输入一律走占位符，
 * 并对 LIKE 通配符做转义，避免模糊查询被注入或误匹配。
 */
public final class JdbcItemDao implements ItemDao {

    private static final String COLUMNS = "id, owner_id, category_id, title, description, item_type, price,"
            + " item_condition, status, tags, image_path, view_count, created_at, updated_at";

    private final Database database;

    public JdbcItemDao(Database database) {
        this.database = database;
    }

    @Override
    public long insert(Item item) {
        String sql = "INSERT INTO items (owner_id, category_id, title, description, item_type, price,"
                + " item_condition, status, tags, image_path, view_count, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.getOwnerId());
            statement.setLong(2, item.getCategoryId());
            statement.setString(3, item.getTitle());
            setNullableString(statement, 4, item.getDescription());
            statement.setString(5, item.getItemType().name());
            statement.setDouble(6, item.getPrice());
            statement.setString(7, item.getCondition().name());
            statement.setString(8, item.getStatus().name());
            setNullableString(statement, 9, joinTags(item.getTags()));
            setNullableString(statement, 10, item.getImagePath());
            statement.setInt(11, item.getViewCount());
            statement.setString(12, Times.toText(item.getCreatedAt()));
            statement.setString(13, Times.toText(item.getUpdatedAt()));
            statement.executeUpdate();
            return JdbcUserDao.readGeneratedKey(connection);
        } catch (SQLException e) {
            throw new DataAccessException("新增物品失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int update(Item item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("更新物品时 id 不能为空");
        }
        String sql = "UPDATE items SET category_id = ?, title = ?, description = ?, item_type = ?, price = ?,"
                + " item_condition = ?, status = ?, tags = ?, image_path = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.getCategoryId());
            statement.setString(2, item.getTitle());
            setNullableString(statement, 3, item.getDescription());
            statement.setString(4, item.getItemType().name());
            statement.setDouble(5, item.getPrice());
            statement.setString(6, item.getCondition().name());
            statement.setString(7, item.getStatus().name());
            setNullableString(statement, 8, joinTags(item.getTags()));
            setNullableString(statement, 9, item.getImagePath());
            statement.setString(10, Times.toText(item.getUpdatedAt()));
            statement.setLong(11, item.getId());
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新物品失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Item> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM items WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询物品失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<Item> search(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM items WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (criteria.getStatus() != null) {
            sql.append(" AND status = ?");
            parameters.add(criteria.getStatus().name());
        }
        if (criteria.getCategoryId() != null) {
            sql.append(" AND category_id = ?");
            parameters.add(criteria.getCategoryId());
        }
        if (criteria.getOwnerId() != null) {
            sql.append(" AND owner_id = ?");
            parameters.add(criteria.getOwnerId());
        }
        if (criteria.getItemType() != null) {
            sql.append(" AND item_type = ?");
            parameters.add(criteria.getItemType().name());
        }
        if (criteria.getMinPrice() != null) {
            sql.append(" AND price >= ?");
            parameters.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append(" AND price <= ?");
            parameters.add(criteria.getMaxPrice());
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String pattern = "%" + JdbcUserDao.escapeLike(criteria.getKeyword().trim()) + "%";
            sql.append(" AND (title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\' OR tags LIKE ? ESCAPE '\\')");
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
        parameters.add(criteria.getLimit());

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DataAccessException("检索物品失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int updateStatus(long itemId, ItemStatus status) {
        String sql = "UPDATE items SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, Times.toText(java.time.LocalDateTime.now()));
            statement.setLong(3, itemId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新物品状态失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void incrementViewCount(long itemId) {
        String sql = "UPDATE items SET view_count = view_count + 1 WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新浏览量失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int deleteById(long itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("删除物品失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int countByOwner(long ownerId) {
        String sql = "SELECT COUNT(*) FROM items WHERE owner_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ownerId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("统计用户物品数失败：" + e.getMessage(), e);
        }
    }

    private static Item mapRow(ResultSet rs) throws SQLException {
        return Item.builder()
                .id(rs.getLong("id"))
                .ownerId(rs.getLong("owner_id"))
                .categoryId(rs.getLong("category_id"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .itemType(ItemType.valueOf(rs.getString("item_type")))
                .price(rs.getDouble("price"))
                .condition(ItemCondition.valueOf(rs.getString("item_condition")))
                .status(ItemStatus.valueOf(rs.getString("status")))
                .tags(splitTags(rs.getString("tags")))
                .imagePath(rs.getString("image_path"))
                .viewCount(rs.getInt("view_count"))
                .createdAt(Times.parse(rs.getString("created_at")))
                .updatedAt(Times.parse(rs.getString("updated_at")))
                .build();
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }

    private static List<String> splitTags(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(text.split(",")));
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
