package com.campusloop.service.sort;

import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemSortersTest {

    private static Item item(long id, String title, double price, int views, int daysAgo) {
        LocalDateTime time = LocalDateTime.of(2026, 11, 8, 12, 0).minusDays(daysAgo);
        return Item.builder()
                .id(id)
                .ownerId(1)
                .categoryId(1)
                .title(title)
                .itemType(ItemType.SELL)
                .price(price)
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ONLINE)
                .viewCount(views)
                .createdAt(time)
                .updatedAt(time)
                .build();
    }

    private static List<Item> sample() {
        return new ArrayList<>(Arrays.asList(
                item(1, "旧物品", 100, 5, 10),
                item(2, "新物品", 10, 50, 1),
                item(3, "中物品", 50, 20, 5)));
    }

    @Test
    @DisplayName("最新发布优先")
    void newestFirst() {
        List<Item> items = sample();
        items.sort(ItemSorters.NEWEST.comparator());
        assertEquals(2, items.get(0).getId());
        assertEquals(3, items.get(1).getId());
        assertEquals(1, items.get(2).getId());
    }

    @Test
    @DisplayName("价格升序与降序")
    void priceOrder() {
        List<Item> asc = sample();
        asc.sort(ItemSorters.PRICE_ASC.comparator());
        assertEquals(List.of(2L, 3L, 1L), ids(asc));

        List<Item> desc = sample();
        desc.sort(ItemSorters.PRICE_DESC.comparator());
        assertEquals(List.of(1L, 3L, 2L), ids(desc));
    }

    @Test
    @DisplayName("热门优先按浏览量降序")
    void hottest() {
        List<Item> items = sample();
        items.sort(ItemSorters.HOTTEST.comparator());
        assertEquals(2, items.get(0).getId());
        assertEquals(3, items.get(1).getId());
    }

    @Test
    @DisplayName("按名称取策略，未知名称回退默认策略（不抛异常）")
    void byNameFallback() {
        assertSame(ItemSorters.PRICE_ASC, ItemSorters.byName("价格从低到高"));
        assertSame(ItemSorters.NEWEST, ItemSorters.byName("乱写的排序"));
        assertSame(ItemSorters.NEWEST, ItemSorters.byName(null));
        assertNotNull(ItemSorters.defaultSorter().comparator());
    }

    private static List<Long> ids(List<Item> items) {
        List<Long> ids = new ArrayList<>();
        for (Item item : items) {
            ids.add(item.getId());
        }
        return ids;
    }
}
