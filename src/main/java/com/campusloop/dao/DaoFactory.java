package com.campusloop.dao;

/**
 * DAO 工厂（抽象工厂 + 工厂方法）。
 *
 * <p>业务层通过工厂取得 DAO，而不直接 new 具体 JDBC 实现：将来替换为连接池实现、
 * 内存实现或 MyBatis 实现时，业务代码无需改动。
 */
public interface DaoFactory {

    UserDao userDao();

    ItemDao itemDao();

    CategoryDao categoryDao();

    FavoriteDao favoriteDao();
}
