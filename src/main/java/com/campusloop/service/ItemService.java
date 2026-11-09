package com.campusloop.service;

import com.campusloop.common.exception.NotFoundException;
import com.campusloop.common.exception.PermissionDeniedException;
import com.campusloop.common.exception.ValidationException;
import com.campusloop.common.util.Logs;
import com.campusloop.common.util.ValidationUtils;
import com.campusloop.dao.CategoryDao;
import com.campusloop.dao.DaoFactory;
import com.campusloop.dao.FavoriteDao;
import com.campusloop.dao.ItemDao;
import com.campusloop.dao.UserDao;
import com.campusloop.domain.Category;
import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.SearchCriteria;
import com.campusloop.domain.User;
import com.campusloop.service.dto.ItemCommand;
import com.campusloop.service.sort.ItemSorter;
import com.campusloop.service.sort.ItemSorters;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 物品领域服务：发布、编辑、上下架、检索、收藏、后台违规下架。
 *
 * <p>关键业务规则：
 * <ul>
 *   <li>价格规则：出售必须大于 0，交换/赠送价格恒为 0；</li>
 *   <li>归属规则：只有发布者本人可编辑、下架，管理员可强制移除；</li>
 *   <li>状态规则：已售出/已移除物品不可再编辑；在架物品才能下架；</li>
 *   <li>检索规则：默认仅返回在架物品，条件过滤在数据层完成，排序由策略对象完成。</li>
 * </ul>
 */
public class ItemService {

    private static final Logger LOG = Logs.getLogger(ItemService.class);
    private static final double MAX_PRICE = 100_000;
    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_LENGTH = 12;
    private static final int DEFAULT_LIMIT = 50;

    private final ItemDao itemDao;
    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final FavoriteDao favoriteDao;

    public ItemService(DaoFactory daoFactory) {
        this.itemDao = daoFactory.itemDao();
        this.userDao = daoFactory.userDao();
        this.categoryDao = daoFactory.categoryDao();
        this.favoriteDao = daoFactory.favoriteDao();
    }

    /**
     * 发布闲置物品，返回落库后的完整对象。
     */
    public Item publish(long ownerId, ItemCommand command) {
        User owner = requireActiveUser(ownerId);
        ValidationUtils.requireNonNull(command, "物品信息");
        ValidatedItem validated = validate(command);

        LocalDateTime now = LocalDateTime.now();
        Item item = Item.builder()
                .ownerId(owner.getId())
                .categoryId(validated.category.getId())
                .title(validated.title)
                .description(validated.description)
                .itemType(validated.itemType)
                .price(validated.price)
                .condition(validated.condition)
                .status(ItemStatus.ONLINE)
                .tags(validated.tags)
                .imagePath(validated.imagePath)
                .viewCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        long id = itemDao.insert(item);
        LOG.info("用户 " + owner.getUsername() + " 发布物品 id=" + id + ", title=" + validated.title);
        return getById(id);
    }

    /**
     * 编辑自己发布的物品：保留原状态、创建时间、浏览量。
     */
    public Item update(long operatorId, long itemId, ItemCommand command) {
        Item existing = getById(itemId);
        requireOwnerOrAdmin(operatorId, existing);
        if (existing.getStatus() == ItemStatus.REMOVED) {
            throw new PermissionDeniedException("该物品已被管理员移除，无法编辑");
        }
        if (existing.getStatus() == ItemStatus.SOLD) {
            throw new PermissionDeniedException("该物品已完成流转，无法编辑");
        }
        ValidatedItem validated = validate(command);

        Item updated = existing.toBuilder()
                .categoryId(validated.category.getId())
                .title(validated.title)
                .description(validated.description)
                .itemType(validated.itemType)
                .price(validated.price)
                .condition(validated.condition)
                .tags(validated.tags)
                .imagePath(validated.imagePath)
                .updatedAt(LocalDateTime.now())
                .build();
        int rows = itemDao.update(updated);
        if (rows == 0) {
            throw new NotFoundException("物品不存在，id=" + itemId);
        }
        LOG.info("物品编辑成功，id=" + itemId + ", operatorId=" + operatorId);
        return getById(itemId);
    }

    /**
     * 下架自己发布的物品。
     */
    public Item offShelf(long operatorId, long itemId) {
        Item existing = getById(itemId);
        requireOwner(operatorId, existing);
        if (existing.getStatus() != ItemStatus.ONLINE) {
            throw new ValidationException("状态", "只有在架物品才能下架，当前状态：" + existing.getStatus());
        }
        itemDao.updateStatus(itemId, ItemStatus.OFF_SHELF);
        LOG.info("物品下架，id=" + itemId + ", operatorId=" + operatorId);
        return getById(itemId);
    }

    /**
     * 重新上架已下架物品。
     */
    public Item relist(long operatorId, long itemId) {
        Item existing = getById(itemId);
        requireOwner(operatorId, existing);
        if (existing.getStatus() != ItemStatus.OFF_SHELF) {
            throw new ValidationException("状态", "只有已下架物品才能重新上架，当前状态：" + existing.getStatus());
        }
        itemDao.updateStatus(itemId, ItemStatus.ONLINE);
        LOG.info("物品重新上架，id=" + itemId + ", operatorId=" + operatorId);
        return getById(itemId);
    }

    /**
     * 管理员强制下架违规物品（软删除，保留流转审计痕迹）。
     */
    public Item removeByAdmin(long operatorId, long itemId) {
        User operator = requireActiveUser(operatorId);
        if (!operator.isAdmin()) {
            throw new PermissionDeniedException("该操作需要管理员权限");
        }
        getById(itemId);
        itemDao.updateStatus(itemId, ItemStatus.REMOVED);
        LOG.info("管理员 " + operator.getUsername() + " 强制移除物品 id=" + itemId);
        return getById(itemId);
    }

    /**
     * 查看物品详情并累计浏览量（详情页调用一次）。
     */
    public Item view(long itemId) {
        Item item = getById(itemId);
        itemDao.incrementViewCount(itemId);
        return item.toBuilder().viewCount(item.getViewCount() + 1).build();
    }

    public Item getById(long itemId) {
        ValidationUtils.requirePositiveId(itemId, "物品ID");
        return itemDao.findById(itemId)
                .orElseThrow(() -> new NotFoundException("物品不存在，id=" + itemId));
    }

    /**
     * 条件检索 + 策略排序。
     */
    public List<Item> search(SearchCriteria criteria, ItemSorter sorter) {
        SearchCriteria effective = criteria == null ? SearchCriteria.builder().build() : criteria;
        ItemSorter effectiveSorter = sorter == null ? ItemSorters.defaultSorter() : sorter;
        List<Item> items = itemDao.search(effective);
        List<Item> sorted = new ArrayList<>(items);
        Comparator<Item> comparator = effectiveSorter.comparator();
        if (comparator != null) {
            sorted.sort(comparator);
        }
        return sorted;
    }

    public List<Item> search(SearchCriteria criteria) {
        return search(criteria, ItemSorters.defaultSorter());
    }

    public List<Item> listByOwner(long ownerId) {
        ValidationUtils.requirePositiveId(ownerId, "用户ID");
        SearchCriteria criteria = SearchCriteria.builder()
                .ownerId(ownerId)
                .status(null)
                .limit(DEFAULT_LIMIT)
                .build();
        return search(criteria, ItemSorters.NEWEST);
    }

    /**
     * 收藏物品：物品不存在或已下架时不允收藏。
     */
    public boolean favorite(long userId, long itemId) {
        requireActiveUser(userId);
        Item item = getById(itemId);
        if (!item.isOnline()) {
            throw new ValidationException("物品", "只能收藏在架物品");
        }
        return favoriteDao.add(userId, itemId);
    }

    /**
     * 取消收藏。
     */
    public boolean unfavorite(long userId, long itemId) {
        ValidationUtils.requirePositiveId(userId, "用户ID");
        ValidationUtils.requirePositiveId(itemId, "物品ID");
        return favoriteDao.remove(userId, itemId);
    }

    public boolean isFavorite(long userId, long itemId) {
        return favoriteDao.exists(userId, itemId);
    }

    /**
     * 我的收藏（按收藏时间倒序）。
     */
    public List<Item> listFavorites(long userId) {
        requireActiveUser(userId);
        List<Item> items = new ArrayList<>();
        for (Long itemId : favoriteDao.findItemIdsByUser(userId)) {
            itemDao.findById(itemId).ifPresent(items::add);
        }
        return items;
    }

    public int countByOwner(long ownerId) {
        return itemDao.countByOwner(ownerId);
    }

    // ---------------------------------------------------------------- 内部校验

    private User requireActiveUser(long userId) {
        ValidationUtils.requirePositiveId(userId, "用户ID");
        User user = userDao.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id=" + userId));
        if (!user.isActive()) {
            throw new PermissionDeniedException("账号已被封禁，无法执行该操作");
        }
        return user;
    }

    private void requireOwner(long operatorId, Item item) {
        User operator = requireActiveUser(operatorId);
        if (item.getOwnerId() != operator.getId()) {
            throw new PermissionDeniedException("只能操作自己发布的物品");
        }
    }

    private void requireOwnerOrAdmin(long operatorId, Item item) {
        User operator = requireActiveUser(operatorId);
        if (item.getOwnerId() != operator.getId() && !operator.isAdmin()) {
            throw new PermissionDeniedException("只能操作自己发布的物品");
        }
    }

    /**
     * 表单校验并归一化：校验与归一化集中在这里，避免各入口重复。
     */
    private ValidatedItem validate(ItemCommand command) {
        if (command == null) {
            throw new ValidationException("物品", "物品信息不能为空");
        }
        ValidatedItem result = new ValidatedItem();
        result.title = ValidationUtils.requireText(command.getTitle(), "标题", 2, 60);
        result.description = ValidationUtils.optionalText(command.getDescription(), "描述", 500);
        result.itemType = command.getItemType() == null ? ItemType.SELL : command.getItemType();
        result.condition = command.getCondition() == null ? ItemCondition.GOOD : command.getCondition();

        if (command.getCategoryId() == null) {
            throw new ValidationException("分类", "请选择物品分类");
        }
        long categoryId = ValidationUtils.requirePositiveId(command.getCategoryId(), "分类");
        result.category = categoryDao.findById(categoryId)
                .orElseThrow(() -> new ValidationException("分类", "所选分类不存在"));

        double price = ValidationUtils.requireRange(command.getPrice(), "价格", 0, MAX_PRICE);
        if (result.itemType == ItemType.SELL && price <= 0) {
            throw new ValidationException("价格", "出售物品的价格必须大于 0");
        }
        if (result.itemType == ItemType.FREE && price != 0) {
            throw new ValidationException("价格", "赠送物品的价格必须为 0");
        }
        if (result.itemType == ItemType.EXCHANGE && price != 0) {
            throw new ValidationException("价格", "交换物品的价格必须为 0");
        }
        result.price = result.itemType == ItemType.SELL ? price : 0;

        result.tags = normalizeTags(command.getTags());
        result.imagePath = ValidationUtils.optionalText(command.getImagePath(), "图片路径", 255);
        return result;
    }

    static List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String tag : rawTags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.length() > MAX_TAG_LENGTH) {
                throw new ValidationException("标签", "单个标签长度不能超过 " + MAX_TAG_LENGTH + " 个字符：" + trimmed);
            }
            unique.add(trimmed);
        }
        if (unique.size() > MAX_TAGS) {
            throw new ValidationException("标签", "标签数量不能超过 " + MAX_TAGS + " 个");
        }
        return new ArrayList<>(unique);
    }

    private static final class ValidatedItem {
        private String title;
        private String description;
        private ItemType itemType;
        private double price;
        private ItemCondition condition;
        private List<String> tags;
        private String imagePath;
        private Category category;
    }
}
