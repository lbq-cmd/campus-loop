package com.campusloop.dao.jdbc;

import com.campusloop.common.Database;
import com.campusloop.dao.CategoryDao;
import com.campusloop.dao.DaoFactory;
import com.campusloop.dao.FavoriteDao;
import com.campusloop.dao.ItemDao;
import com.campusloop.dao.UserDao;

/**
 * SQLite 版 DAO 工厂（工厂方法的具体实现 + 单例）。
 *
 * <p>DAO 实例在工厂内缓存复用，避免每次业务调用重复创建对象；DAO 本身无可变状态，可安全共享。
 */
public final class SqliteDaoFactory implements DaoFactory {

    private static volatile SqliteDaoFactory defaultInstance;

    private final UserDao userDao;
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;
    private final FavoriteDao favoriteDao;

    public SqliteDaoFactory(Database database) {
        this.userDao = new JdbcUserDao(database);
        this.itemDao = new JdbcItemDao(database);
        this.categoryDao = new JdbcCategoryDao(database);
        this.favoriteDao = new JdbcFavoriteDao(database);
    }

    /** 应用默认工厂（使用默认数据库）。 */
    public static SqliteDaoFactory getDefault() {
        SqliteDaoFactory local = defaultInstance;
        if (local == null) {
            synchronized (SqliteDaoFactory.class) {
                local = defaultInstance;
                if (local == null) {
                    local = new SqliteDaoFactory(Database.getDefault());
                    defaultInstance = local;
                }
            }
        }
        return local;
    }

    @Override
    public UserDao userDao() {
        return userDao;
    }

    @Override
    public ItemDao itemDao() {
        return itemDao;
    }

    @Override
    public CategoryDao categoryDao() {
        return categoryDao;
    }

    @Override
    public FavoriteDao favoriteDao() {
        return favoriteDao;
    }
}
