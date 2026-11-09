package com.campusloop.service;

import com.campusloop.common.exception.NotFoundException;
import com.campusloop.common.exception.PermissionDeniedException;
import com.campusloop.common.exception.ValidationException;
import com.campusloop.domain.Category;
import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.SearchCriteria;
import com.campusloop.support.TestData;
import com.campusloop.support.TestDb;
import com.campusloop.service.dto.ItemCommand;
import com.campusloop.service.sort.ItemSorters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {

    private TestDb testDb;
    private ItemService itemService;
    private long ownerId;
    private long otherId;
    private long categoryId;

    @BeforeEach
    void setUp() {
        testDb = new TestDb();
        itemService = new ItemService(testDb.daoFactory());
        ownerId = TestData.createUser(testDb.userDao(), "owner01");
        otherId = TestData.createUser(testDb.userDao(), "owner02");
        categoryId = TestData.category(testDb.categoryDao(), "数码配件").getId();
    }

    @AfterEach
    void tearDown() {
        testDb.cleanup();
    }

    private ItemCommand sellCommand(String title, double price) {
        return new ItemCommand()
                .setTitle(title)
                .setCategoryId(categoryId)
                .setDescription("九成新，宿舍当面交易")
                .setItemType(ItemType.SELL)
                .setPrice(price)
                .setCondition(ItemCondition.LIKE_NEW)
                .setTags(new ArrayList<>(Arrays.asList("数码", "数码", " 耳机 ")));
    }

    @Test
    @DisplayName("发布出售物品：状态在架、标签去重去空格、成色与描述落库")
    void publishSellItem() {
        Item item = itemService.publish(ownerId, sellCommand("二手蓝牙耳机", 99));
        assertTrue(item.getId() > 0);
        assertEquals(ItemStatus.ONLINE, item.getStatus());
        assertEquals(ItemType.SELL, item.getItemType());
        assertEquals(List.of("数码", "耳机"), item.getTags());
        assertEquals(ownerId, item.getOwnerId());
        assertEquals(0, item.getViewCount());
    }

    @Test
    @DisplayName("发布赠送物品：价格必须为 0")
    void publishFreeItem() {
        ItemCommand free = new ItemCommand().setTitle("多余衣架").setCategoryId(categoryId)
                .setItemType(ItemType.FREE).setPrice(0);
        Item item = itemService.publish(ownerId, free);
        assertEquals(0, item.getPrice(), 0.001);
        assertEquals(ItemType.FREE, item.getItemType());

        ItemCommand illegal = new ItemCommand().setTitle("多余衣架").setCategoryId(categoryId)
                .setItemType(ItemType.FREE).setPrice(5);
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, illegal));
    }

    @Test
    @DisplayName("出售价格必须大于 0，交换物品价格必须为 0")
    void publishPriceRules() {
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, sellCommand("零元耳机", 0)));
        ItemCommand exchangeWithPrice = new ItemCommand().setTitle("交换台灯").setCategoryId(categoryId)
                .setItemType(ItemType.EXCHANGE).setPrice(10);
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, exchangeWithPrice));
    }

    @Test
    @DisplayName("标题过短、价格越界、标签过多或过长均被拒绝")
    void publishValidation() {
        ItemCommand shortTitle = sellCommand("灯", 10);
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, shortTitle));

        ItemCommand tooExpensive = sellCommand("电脑", 100_001);
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, tooExpensive));

        ItemCommand tooManyTags = sellCommand("耳机", 10)
                .setTags(Arrays.asList("a1", "a2", "a3", "a4", "a5", "a6"));
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, tooManyTags));

        ItemCommand longTag = sellCommand("耳机", 10).setTags(List.of("这是一个非常非常长的标签名称"));
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, longTag));
    }

    @Test
    @DisplayName("分类不存在时拒绝发布")
    void publishRequiresExistingCategory() {
        ItemCommand cmd = sellCommand("耳机", 10).setCategoryId(999999L);
        assertThrows(ValidationException.class, () -> itemService.publish(ownerId, cmd));
    }

    @Test
    @DisplayName("被封禁用户不能发布物品")
    void bannedUserCannotPublish() {
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        new UserService(testDb.daoFactory()).changeUserStatus(adminId, ownerId, true);
        assertThrows(PermissionDeniedException.class, () -> itemService.publish(ownerId, sellCommand("耳机", 10)));
    }

    @Test
    @DisplayName("编辑物品：本人可改，保留创建时间与浏览量")
    void updateOwnItem() {
        Item item = itemService.publish(ownerId, sellCommand("旧标题", 50));
        itemService.view(item.getId());
        Item updated = itemService.update(ownerId, item.getId(), sellCommand("新标题", 60));
        assertEquals("新标题", updated.getTitle());
        assertEquals(60, updated.getPrice(), 0.001);
        assertEquals(item.getCreatedAt(), updated.getCreatedAt());
        assertEquals(1, updated.getViewCount());
    }

    @Test
    @DisplayName("非本人不能编辑物品，管理员可以")
    void updatePermission() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        assertThrows(PermissionDeniedException.class,
                () -> itemService.update(otherId, item.getId(), sellCommand("恶意修改", 1)));

        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        Item updated = itemService.update(adminId, item.getId(), sellCommand("管理员修正", 45));
        assertEquals("管理员修正", updated.getTitle());
    }

    @Test
    @DisplayName("已移除物品不可编辑")
    void removedItemCannotBeEdited() {
        Item item = itemService.publish(ownerId, sellCommand("违规物品", 50));
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        itemService.removeByAdmin(adminId, item.getId());
        assertThrows(PermissionDeniedException.class,
                () -> itemService.update(ownerId, item.getId(), sellCommand("改回来", 10)));
    }

    @Test
    @DisplayName("下架与重新上架的状态流转")
    void offShelfAndRelist() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        Item off = itemService.offShelf(ownerId, item.getId());
        assertEquals(ItemStatus.OFF_SHELF, off.getStatus());
        assertThrows(ValidationException.class, () -> itemService.offShelf(ownerId, item.getId()));

        Item relisted = itemService.relist(ownerId, item.getId());
        assertEquals(ItemStatus.ONLINE, relisted.getStatus());
        assertThrows(ValidationException.class, () -> itemService.relist(ownerId, item.getId()));
    }

    @Test
    @DisplayName("非本人不能下架，管理员强制移除后物品不再出现在市场")
    void offShelfPermissionAndAdminRemove() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        assertThrows(PermissionDeniedException.class, () -> itemService.offShelf(otherId, item.getId()));

        assertThrows(PermissionDeniedException.class, () -> itemService.removeByAdmin(otherId, item.getId()));
        long adminId = TestData.createAdmin(testDb.userDao(), "root");
        Item removed = itemService.removeByAdmin(adminId, item.getId());
        assertEquals(ItemStatus.REMOVED, removed.getStatus());
        assertTrue(itemService.search(SearchCriteria.builder().keyword("耳机").build()).isEmpty());
    }

    @Test
    @DisplayName("检索：关键词命中 + 价格排序策略生效")
    void searchWithSorter() {
        itemService.publish(ownerId, sellCommand("机械键盘", 200));
        itemService.publish(otherId, sellCommand("键盘手托", 30));
        itemService.publish(ownerId, sellCommand("蓝牙耳机", 120));

        List<Item> byKeyword = itemService.search(SearchCriteria.builder().keyword("键盘").build());
        assertEquals(2, byKeyword.size());

        List<Item> asc = itemService.search(SearchCriteria.builder().build(), ItemSorters.PRICE_ASC);
        assertEquals(30, asc.get(0).getPrice(), 0.001);
        assertTrue(asc.get(0).getPrice() <= asc.get(1).getPrice());

        List<Item> desc = itemService.search(SearchCriteria.builder().build(), ItemSorters.PRICE_DESC);
        assertEquals(200, desc.get(0).getPrice(), 0.001);
    }

    @Test
    @DisplayName("我的发布包含已下架物品，按创建时间倒序")
    void listByOwnerIncludesOffShelf() {
        Item first = itemService.publish(ownerId, sellCommand("物品一", 10));
        Item second = itemService.publish(ownerId, sellCommand("物品二", 20));
        itemService.offShelf(ownerId, second.getId());

        List<Item> mine = itemService.listByOwner(ownerId);
        assertEquals(2, mine.size());
        assertEquals(second.getId(), mine.get(0).getId());
        assertEquals(first.getId(), mine.get(1).getId());
    }

    @Test
    @DisplayName("收藏：只能收藏在架物品，取消后列表移除")
    void favoriteFlow() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        assertTrue(itemService.favorite(otherId, item.getId()));
        assertTrue(itemService.isFavorite(otherId, item.getId()));
        assertEquals(1, itemService.listFavorites(otherId).size());

        assertTrue(itemService.unfavorite(otherId, item.getId()));
        assertFalse(itemService.isFavorite(otherId, item.getId()));

        itemService.offShelf(ownerId, item.getId());
        assertThrows(ValidationException.class, () -> itemService.favorite(otherId, item.getId()));
    }

    @Test
    @DisplayName("浏览量累计")
    void viewIncrementsCounter() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        Item viewed = itemService.view(item.getId());
        assertEquals(1, viewed.getViewCount());
        assertEquals(2, itemService.view(item.getId()).getViewCount());
    }

    @Test
    @DisplayName("统计用户发布数量")
    void countByOwner() {
        itemService.publish(ownerId, sellCommand("耳机", 50));
        itemService.publish(ownerId, sellCommand("键盘", 60));
        assertEquals(2, itemService.countByOwner(ownerId));
    }

    @Test
    @DisplayName("查询不存在的物品抛出 NotFoundException")
    void findMissingItem() {
        assertThrows(NotFoundException.class, () -> itemService.getById(99999));
    }

    @Test
    @DisplayName("编辑时也可更换分类")
    void updateCategory() {
        Item item = itemService.publish(ownerId, sellCommand("耳机", 50));
        Category books = TestData.category(testDb.categoryDao(), "教材书籍");
        ItemCommand cmd = sellCommand("耳机", 50).setCategoryId(books.getId());
        Item updated = itemService.update(ownerId, item.getId(), cmd);
        assertEquals(books.getId(), updated.getCategoryId());
    }
}
