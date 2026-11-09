package com.campusloop.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 闲置物品实体。
 *
 * <p>采用建造者模式（Builder）构造：物品字段多、存在大量可选字段（描述、标签、图片），
 * 使用链式建造者可以避免超长构造参数列表，并在 build 时统一做必填项检查，
 * 使对象一旦创建即处于合法状态。
 */
public final class Item {

    private final Long id;
    private final long ownerId;
    private final long categoryId;
    private final String title;
    private final String description;
    private final ItemType itemType;
    private final double price;
    private final ItemCondition condition;
    private final ItemStatus status;
    private final List<String> tags;
    private final String imagePath;
    private final int viewCount;
    private final java.time.LocalDateTime createdAt;
    private final java.time.LocalDateTime updatedAt;

    private Item(Builder builder) {
        this.id = builder.id;
        this.ownerId = builder.ownerId;
        this.categoryId = builder.categoryId;
        this.title = builder.title;
        this.description = builder.description;
        this.itemType = builder.itemType;
        this.price = builder.price;
        this.condition = builder.condition;
        this.status = builder.status;
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.imagePath = builder.imagePath;
        this.viewCount = builder.viewCount;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .ownerId(ownerId)
                .categoryId(categoryId)
                .title(title)
                .description(description)
                .itemType(itemType)
                .price(price)
                .condition(condition)
                .status(status)
                .tags(tags)
                .imagePath(imagePath)
                .viewCount(viewCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public Long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public double getPrice() {
        return price;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getViewCount() {
        return viewCount;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOnline() {
        return status == ItemStatus.ONLINE;
    }

    @Override
    public String toString() {
        return "Item{id=" + id + ", title='" + title + "', type=" + itemType + ", price=" + price
                + ", status=" + status + ", tags=" + tags + '}';
    }

    /**
     * 物品建造者：必填字段缺失时在 build() 立即失败（fail-fast），避免半成品对象流入数据库。
     */
    public static final class Builder {
        private Long id;
        private Long ownerId;
        private Long categoryId;
        private String title;
        private String description;
        private ItemType itemType;
        private Double price;
        private ItemCondition condition = ItemCondition.GOOD;
        private ItemStatus status = ItemStatus.ONLINE;
        private List<String> tags = new ArrayList<>();
        private String imagePath;
        private int viewCount;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder ownerId(long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder categoryId(long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder itemType(ItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder price(double price) {
            this.price = price;
            return this;
        }

        public Builder condition(ItemCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder status(ItemStatus status) {
            this.status = status;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
            return this;
        }

        public Builder imagePath(String imagePath) {
            this.imagePath = imagePath;
            return this;
        }

        public Builder viewCount(int viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public Builder createdAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Item build() {
            Objects.requireNonNull(ownerId, "物品必须指定发布者 ownerId");
            Objects.requireNonNull(categoryId, "物品必须指定分类 categoryId");
            Objects.requireNonNull(itemType, "物品必须指定流转方式 itemType");
            Objects.requireNonNull(price, "物品必须指定价格 price");
            if (title == null || title.isBlank()) {
                throw new IllegalStateException("物品标题不能为空");
            }
            if (price < 0) {
                throw new IllegalStateException("物品价格不能为负数");
            }
            if (createdAt == null) {
                createdAt = java.time.LocalDateTime.now();
            }
            if (updatedAt == null) {
                updatedAt = createdAt;
            }
            return new Item(this);
        }
    }
}
