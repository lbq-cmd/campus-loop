package com.campusloop.support;

import com.campusloop.common.Database;
import com.campusloop.common.SchemaLoader;
import com.campusloop.dao.CategoryDao;
import com.campusloop.dao.DaoFactory;
import com.campusloop.dao.FavoriteDao;
import com.campusloop.dao.ItemDao;
import com.campusloop.dao.UserDao;
import com.campusloop.dao.jdbc.SqliteDaoFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 测试数据库夹具：每个测试方法使用独立临时库，结构与交付脚本完全一致，互不污染。
 */
public final class TestDb {

    private final Path directory;
    private final Database database;
    private final DaoFactory daoFactory;

    public TestDb() {
        try {
            this.directory = Files.createTempDirectory("campusloop-test-");
        } catch (IOException e) {
            throw new IllegalStateException("创建测试临时目录失败", e);
        }
        this.database = new Database("jdbc:sqlite:" + directory.resolve("test.db").toAbsolutePath());
        SchemaLoader.createSchema(database);
        SchemaLoader.loadSeed(database);
        this.daoFactory = new SqliteDaoFactory(database);
    }

    public Database database() {
        return database;
    }

    public DaoFactory daoFactory() {
        return daoFactory;
    }

    public UserDao userDao() {
        return daoFactory.userDao();
    }

    public ItemDao itemDao() {
        return daoFactory.itemDao();
    }

    public CategoryDao categoryDao() {
        return daoFactory.categoryDao();
    }

    public FavoriteDao favoriteDao() {
        return daoFactory.favoriteDao();
    }

    /** 删除临时数据库文件，避免测试残留。 */
    public void cleanup() {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 临时文件删除失败不影响测试结果
                }
            });
        } catch (IOException ignored) {
            // 同上
        }
    }
}
