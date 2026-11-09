package com.campusloop.dao.jdbc;

import com.campusloop.support.TestData;
import com.campusloop.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcFavoriteDaoTest {

    private TestDb testDb;
    private JdbcFavoriteDao favoriteDao;
    private long userId;
    private long ownerId;
    private long categoryId;
    private long itemId;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        favoriteDao = new JdbcFavoriteDao(testDb.database());
        userId = TestData.createUser(testDb.userDao(), "collector");
        ownerId = TestData.createUser(testDb.userDao(), "publisher");
        categoryId = TestData.category(testDb.categoryDao(), "教材书籍").getId();
        itemId = TestData.createItem(testDb.itemDao(), ownerId, categoryId, "线性代数教材", 15);
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    @Test
    @DisplayName("重复收藏幂等：第二次返回 false，记录仍只有一条")
    void addIsIdempotent() {
        assertTrue(favoriteDao.add(userId, itemId));
        assertFalse(favoriteDao.add(userId, itemId));
        assertEquals(1, favoriteDao.findItemIdsByUser(userId).size());
    }

    @Test
    @DisplayName("取消收藏与状态查询")
    void removeAndExists() {
        favoriteDao.add(userId, itemId);
        assertTrue(favoriteDao.exists(userId, itemId));
        assertTrue(favoriteDao.remove(userId, itemId));
        assertFalse(favoriteDao.exists(userId, itemId));
        assertFalse(favoriteDao.remove(userId, itemId));
    }

    @Test
    @DisplayName("收藏列表与物品收藏计数")
    void listAndCount() {
        long secondItem = TestData.createItem(testDb.itemDao(), ownerId, categoryId, "台灯", 20);
        favoriteDao.add(userId, itemId);
        favoriteDao.add(userId, secondItem);

        List<Long> ids = favoriteDao.findItemIdsByUser(userId);
        assertEquals(2, ids.size());
        assertEquals(1, favoriteDao.countByItem(itemId));
        assertTrue(favoriteDao.findItemIdsByUser(99999).isEmpty());
    }
}
