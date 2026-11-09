package com.campusloop.service.sort;

import com.campusloop.domain.Item;

import java.util.Comparator;

/**
 * 物品排序策略（策略模式）。
 *
 * <p>把「怎么排序」从 ItemService 的检索流程中抽离：新增排序方式只需新增一个实现，
 * 服务层与界面层均无需修改，符合开闭原则。
 */
public interface ItemSorter {

    /** 排序方式名称，供界面下拉框展示。 */
    String getName();

    /** 排序比较器。 */
    Comparator<Item> comparator();
}
