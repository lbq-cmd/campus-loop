package com.campusloop.support;

import com.campusloop.common.util.PasswordEncoder;
import com.campusloop.dao.CategoryDao;
import com.campusloop.dao.ItemDao;
import com.campusloop.dao.UserDao;
import com.campusloop.domain.Category;
import com.campusloop.domain.Item;
import com.campusloop.domain.ItemCondition;
import com.campusloop.domain.ItemStatus;
import com.campusloop.domain.ItemType;
import com.campusloop.domain.Role;
import com.campusloop.domain.User;
import com.campusloop.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 测试数据工厂：集中构造测试实体，避免各测试类重复拼装。
 */
public final class TestData {

    private static final PasswordEncoder ENCODER = new PasswordEncoder();

    private TestData() {
    }

    public static User user(String username) {
        return new User(null, username, ENCODER.encode("Passw0rd!"), "昵称" + username,
                "13800000000", Role.STUDENT, UserStatus.ACTIVE, 100, LocalDateTime.now(), null);
    }

    public static User admin(String username) {
        return new User(null, username, ENCODER.encode("Passw0rd!"), "管理" + username,
                null, Role.ADMIN, UserStatus.ACTIVE, 100, LocalDateTime.now(), null);
    }

    public static long createUser(UserDao dao, String username) {
        return dao.insert(user(username));
    }

    public static long createAdmin(UserDao dao, String username) {
        return dao.insert(admin(username));
    }

    /**
     * 取得分类：脚本已内置的同名分类直接复用，否则新建，避免唯一约束冲突。
     */
    public static Category category(CategoryDao dao, String name) {
        return dao.findByName(name).orElseGet(() -> {
            long id = dao.insert(new Category(null, name, name + "描述", 10));
            return new Category(id, name, name + "描述", 10);
        });
    }

    public static Item item(long ownerId, long categoryId, String title, double price) {
        LocalDateTime now = LocalDateTime.now();
        return Item.builder()
                .ownerId(ownerId)
                .categoryId(categoryId)
                .title(title)
                .description("测试描述：" + title)
                .itemType(ItemType.SELL)
                .price(price)
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ONLINE)
                .tags(Arrays.asList("教材", "考研"))
                .viewCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static long createItem(ItemDao dao, long ownerId, long categoryId, String title, double price) {
        return dao.insert(item(ownerId, categoryId, title, price));
    }

    public static List<String> tags(String... values) {
        return Arrays.asList(values);
    }
}
