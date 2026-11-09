package com.campusloop.domain;

import java.util.Objects;

/**
 * 物品分类（值对象：以 id 区分，名称由管理员维护）。
 */
public final class Category {

    private final Long id;
    private final String name;
    private final String description;
    private final int sortOrder;

    public Category(Long id, String name, String description, int sortOrder) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "category.name");
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public String toString() {
        return name;
    }
}
