package com.campusloop.domain;

/**
 * 物品检索条件（条件对象 / 参数对象）。
 *
 * <p>检索条件字段较多且大多可选，使用建造者模式组装，避免服务层出现十余个重载方法；
 * 同时把「条件」与「排序」分离，条件负责过滤，排序由策略对象负责，两者可自由组合。
 */
public final class SearchCriteria {

    private final String keyword;
    private final Long categoryId;
    private final Long ownerId;
    private final ItemType itemType;
    private final ItemStatus status;
    private final Double minPrice;
    private final Double maxPrice;
    private final int limit;

    private SearchCriteria(Builder builder) {
        this.keyword = builder.keyword;
        this.categoryId = builder.categoryId;
        this.ownerId = builder.ownerId;
        this.itemType = builder.itemType;
        this.status = builder.status;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.limit = builder.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public int getLimit() {
        return limit;
    }

    public static final class Builder {
        private String keyword;
        private Long categoryId;
        private Long ownerId;
        private ItemType itemType;
        private ItemStatus status = ItemStatus.ONLINE;
        private Double minPrice;
        private Double maxPrice;
        private int limit = 50;

        public Builder keyword(String keyword) {
            this.keyword = keyword == null ? null : keyword.trim();
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder ownerId(Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder itemType(ItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder status(ItemStatus status) {
            this.status = status;
            return this;
        }

        public Builder minPrice(Double minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public SearchCriteria build() {
            if (limit <= 0) {
                throw new IllegalArgumentException("查询条数上限必须大于 0");
            }
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                throw new IllegalArgumentException("最低价不能高于最高价");
            }
            return new SearchCriteria(this);
        }
    }
}
