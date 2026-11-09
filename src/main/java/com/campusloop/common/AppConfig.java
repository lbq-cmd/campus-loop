package com.campusloop.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置读取器：优先系统属性，其次 classpath:app.properties，最后内置默认值。
 * 采用单例（饿汉、线程安全），避免各处重复读取配置文件。
 */
public final class AppConfig {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:data/campusloop.db";

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载 app.properties 失败", e);
        }
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    /** 数据库连接地址，可用 -Dcampusloop.db.url 覆盖（测试与演示常用）。 */
    public String getDbUrl() {
        String override = System.getProperty("campusloop.db.url");
        if (override != null && !override.isBlank()) {
            return override;
        }
        return properties.getProperty("db.url", DEFAULT_DB_URL).trim();
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
