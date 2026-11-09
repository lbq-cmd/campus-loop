package com.campusloop.service.dto;

import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品发布/编辑表单对象（后续迭代的 Swing 表单可直接填充本对象后交给服务层）。
 */
public class ItemCommand {

    private String title;
    private Long categoryId;
    private String description;
    private ItemType itemType = ItemType.SELL;
    private double price;
    private ItemCondition condition = ItemCondition.GOOD;
    private List<String> tags = new ArrayList<>();
    private String imagePath;

    public String getTitle() {
        return title;
    }

    public ItemCommand setTitle(String title) {
        this.title = title;
        return this;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public ItemCommand setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ItemCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public ItemCommand setItemType(ItemType itemType) {
        this.itemType = itemType;
        return this;
    }

    public double getPrice() {
        return price;
    }

    public ItemCommand setPrice(double price) {
        this.price = price;
        return this;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public ItemCommand setCondition(ItemCondition condition) {
        this.condition = condition;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public ItemCommand setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        return this;
    }

    public String getImagePath() {
        return imagePath;
    }

    public ItemCommand setImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }
}
