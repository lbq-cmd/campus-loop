package com.campusloop.service.sort;

import com.campusloop.domain.Item;

import java.util.Comparator;

/**
 * 内置排序策略集合，界面层通过 {@link #byName(String)} 直接取得策略实例。
 */
public final class ItemSorters {

    /** 最新发布优先（默认）。 */
    public static final ItemSorter NEWEST = new SimpleSorter("最新发布",
            Comparator.comparing(Item::getCreatedAt).reversed()
                    .thenComparing(Item::getId, Comparator.reverseOrder()));

    /** 价格从低到高。 */
    public static final ItemSorter PRICE_ASC = new SimpleSorter("价格从低到高",
            Comparator.comparingDouble(Item::getPrice)
                    .thenComparing(Item::getCreatedAt, Comparator.reverseOrder()));

    /** 价格从高到低。 */
    public static final ItemSorter PRICE_DESC = new SimpleSorter("价格从高到低",
            Comparator.comparingDouble(Item::getPrice).reversed()
                    .thenComparing(Item::getCreatedAt, Comparator.reverseOrder()));

    /** 热门优先：浏览量高者靠前。 */
    public static final ItemSorter HOTTEST = new SimpleSorter("最热门",
            Comparator.comparingInt(Item::getViewCount).reversed()
                    .thenComparing(Item::getCreatedAt, Comparator.reverseOrder()));

    private ItemSorters() {
    }

    public static ItemSorter defaultSorter() {
        return NEWEST;
    }

    /**
     * 按名称查找排序策略，未匹配时返回默认策略（防御性编程：不因界面传入脏数据而抛异常）。
     */
    public static ItemSorter byName(String name) {
        if (name == null) {
            return NEWEST;
        }
        for (ItemSorter sorter : new ItemSorter[]{NEWEST, PRICE_ASC, PRICE_DESC, HOTTEST}) {
            if (sorter.getName().equals(name)) {
                return sorter;
            }
        }
        return NEWEST;
    }

    private static final class SimpleSorter implements ItemSorter {
        private final String name;
        private final Comparator<Item> comparator;

        private SimpleSorter(String name, Comparator<Item> comparator) {
            this.name = name;
            this.comparator = comparator;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Comparator<Item> comparator() {
            return comparator;
        }
    }
}
