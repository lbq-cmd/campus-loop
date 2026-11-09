package com.campusloop.common;

import com.campusloop.common.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 初始化脚本加载器：执行 classpath:sql/schema.sql 与 sql/seed.sql。
 * 程序首次启动、测试准备数据库时复用同一份脚本，保证结构与交付脚本一致。
 */
public final class SchemaLoader {

    private static final String SCHEMA_RESOURCE = "/sql/schema.sql";
    private static final String SEED_RESOURCE = "/sql/seed.sql";

    private SchemaLoader() {
    }

    /** 建表（幂等）。 */
    public static void createSchema(Database database) {
        executeScript(database, SCHEMA_RESOURCE);
    }

    /** 写入默认分类（幂等）。 */
    public static void loadSeed(Database database) {
        executeScript(database, SEED_RESOURCE);
    }

    /** 建表 + 默认数据，并确保数据库文件所在目录存在。 */
    public static void initialize(Database database) {
        ensureDirectory(database.getUrl());
        createSchema(database);
        loadSeed(database);
    }

    private static void executeScript(Database database, String resource) {
        List<String> statements = readStatements(resource);
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("初始化数据库脚本失败：" + resource, e);
        }
    }

    static List<String> readStatements(String resource) {
        String text;
        try (InputStream in = SchemaLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new DataAccessException("找不到初始化脚本资源：" + resource);
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("读取初始化脚本失败：" + resource, e);
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1));
                current.setLength(0);
            }
        }
        String rest = current.toString().trim();
        if (!rest.isEmpty()) {
            statements.add(rest);
        }
        return statements;
    }

    private static void ensureDirectory(String jdbcUrl) {
        String prefix = "jdbc:sqlite:";
        if (!jdbcUrl.startsWith(prefix)) {
            return;
        }
        String path = jdbcUrl.substring(prefix.length());
        if (path.isEmpty() || path.startsWith(":memory:") || path.startsWith("file:")) {
            return;
        }
        Path parent = Paths.get(path).toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new DataAccessException("创建数据库目录失败：" + parent, e);
        }
    }
}
