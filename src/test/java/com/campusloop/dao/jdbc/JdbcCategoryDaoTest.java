package com.campusloop.dao.jdbc;

import com.campusloop.domain.Category;
import com.campusloop.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCategoryDaoTest {

    private TestDb testDb;
    private JdbcCategoryDao categoryDao;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        categoryDao = new JdbcCategoryDao(testDb.database());
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    @Test
    @DisplayName("初始化脚本写入 6 个默认分类并按 sort_order 排序")
    void seedCategoriesLoaded() {
        List<Category> categories = categoryDao.findAll();
        assertEquals(6, categories.size());
        assertEquals("教材书籍", categories.get(0).getName());
        assertEquals("其他闲置", categories.get(categories.size() - 1).getName());
    }

    @Test
    @DisplayName("按名称精确查询")
    void findByName() {
        assertTrue(categoryDao.findByName("数码配件").isPresent());
        assertFalse(categoryDao.findByName("不存在分类").isPresent());
        assertTrue(categoryDao.existsByName("文体用品"));
    }

    @Test
    @DisplayName("新增分类后可按 id 查回")
    void insertCategory() {
        long id = categoryDao.insert(new Category(null, "实验器材", "烧杯、万用表", 60));
        Category stored = categoryDao.findById(id).orElseThrow();
        assertEquals("实验器材", stored.getName());
        assertEquals(60, stored.getSortOrder());
    }

    @Test
    @DisplayName("分类名称唯一")
    void duplicateNameRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> categoryDao.insert(new Category(null, "教材书籍", null, 1)));
    }
}
