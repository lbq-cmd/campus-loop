package com.campusloop.dao.jdbc;

import com.campusloop.domain.Item;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.SearchCriteria;
import com.campusloop.support.TestData;
import com.campusloop.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcItemDaoTest {

    private TestDb testDb;
    private JdbcItemDao itemDao;
    private long ownerId;
    private long categoryId;
    private long otherCategoryId;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        itemDao = new JdbcItemDao(testDb.database());
        ownerId = TestData.createUser(testDb.userDao(), "owner01");
        long otherId = TestData.createUser(testDb.userDao(), "owner02");
        categoryId = TestData.category(testDb.categoryDao(), "数码配件").getId();
        otherCategoryId = TestData.category(testDb.categoryDao(), "教材书籍").getId();
        itemDao.insert(TestData.item(ownerId, categoryId, "二手键盘", 80));
        itemDao.insert(TestData.item(ownerId, categoryId, "蓝牙耳机", 120));
        itemDao.insert(TestData.item(otherId, otherCategoryId, "考研数学真题", 30));
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    @Test
    @DisplayName("插入后可按 id 回读，标签与状态完整还原")
    void insertAndRead() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "台灯", 25);
        Item item = itemDao.findById(id).orElseThrow();
        assertEquals("台灯", item.getTitle());
        assertEquals(25, item.getPrice(), 0.001);
        assertEquals(ItemType.SELL, item.getItemType());
        assertEquals(ItemStatus.ONLINE, item.getStatus());
        assertEquals(List.of("教材", "考研"), item.getTags());
        assertFalse(item.getCreatedAt() == null);
    }

    @Test
    @DisplayName("外键约束：分类不存在时插入失败")
    void foreignKeyEnforced() {
        assertThrows(RuntimeException.class,
                () -> itemDao.insert(TestData.item(ownerId, 999999, "非法分类物品", 10)));
    }

    @Test
    @DisplayName("更新物品字段并保留浏览量")
    void updateItem() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "旧标题", 50);
        itemDao.incrementViewCount(id);
        Item item = itemDao.findById(id).orElseThrow();
        Item changed = item.toBuilder().title("新标题").price(65).status(ItemStatus.OFF_SHELF).build();
        assertEquals(1, itemDao.update(changed));

        Item stored = itemDao.findById(id).orElseThrow();
        assertEquals("新标题", stored.getTitle());
        assertEquals(65, stored.getPrice(), 0.001);
        assertEquals(ItemStatus.OFF_SHELF, stored.getStatus());
        assertEquals(1, stored.getViewCount());
    }

    @Test
    @DisplayName("按关键词检索标题、描述与标签")
    void searchByKeyword() {
        assertEquals(1, itemDao.search(SearchCriteria.builder().keyword("键盘").build()).size());
        assertEquals(1, itemDao.search(SearchCriteria.builder().keyword("蓝牙").build()).size());
        assertEquals(3, itemDao.search(SearchCriteria.builder().keyword("考研").build()).size(),
                "三条测试数据的标签都含「考研」，应全部命中");
        assertEquals(0, itemDao.search(SearchCriteria.builder().keyword("不存在的词").build()).size());
    }

    @Test
    @DisplayName("按分类与价格区间组合筛选")
    void searchByCategoryAndPrice() {
        SearchCriteria digital = SearchCriteria.builder().categoryId(categoryId).build();
        assertEquals(2, itemDao.search(digital).size());

        SearchCriteria cheap = SearchCriteria.builder().minPrice(50.0).maxPrice(100.0).build();
        List<Item> result = itemDao.search(cheap);
        assertEquals(1, result.size());
        assertEquals("二手键盘", result.get(0).getTitle());
    }

    @Test
    @DisplayName("默认只查在架物品，status=null 时不过滤状态")
    void statusFilter() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "已下架物品", 10);
        itemDao.updateStatus(id, ItemStatus.OFF_SHELF);
        assertEquals(3, itemDao.search(SearchCriteria.builder().build()).size());
        assertEquals(4, itemDao.search(SearchCriteria.builder().status(null).build()).size());
    }

    @Test
    @DisplayName("下架与浏览量更新")
    void updateStatusAndViewCount() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "待下架", 10);
        itemDao.updateStatus(id, ItemStatus.OFF_SHELF);
        itemDao.incrementViewCount(id);
        itemDao.incrementViewCount(id);
        Item item = itemDao.findById(id).orElseThrow();
        assertEquals(ItemStatus.OFF_SHELF, item.getStatus());
        assertEquals(2, item.getViewCount());
    }

    @Test
    @DisplayName("统计某用户发布数量")
    void countByOwner() {
        assertEquals(2, itemDao.countByOwner(ownerId));
    }

    @Test
    @DisplayName("删除物品后查不到")
    void deleteById() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "待删除", 10);
        assertEquals(1, itemDao.deleteById(id));
        assertFalse(itemDao.findById(id).isPresent());
    }

    @Test
    @DisplayName("查询不存在的物品返回空 Optional")
    void findMissing() {
        assertFalse(itemDao.findById(99999).isPresent());
    }

    @Test
    @DisplayName("收藏与删除共用外键：删除物品后收藏记录级联清理")
    void deleteCascadesFavorites() {
        long id = TestData.createItem(itemDao, ownerId, categoryId, "带收藏的物品", 10);
        testDb.favoriteDao().add(ownerId, id);
        assertTrue(testDb.favoriteDao().exists(ownerId, id));
        itemDao.deleteById(id);
        assertFalse(testDb.favoriteDao().exists(ownerId, id));
    }
}
