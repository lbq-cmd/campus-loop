package com.campusloop.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 日志工具：统一初始化 java.util.logging，关键业务操作写入 logs/campusloop.log。
 * 迭代二只记录登录、发布、下架、后台删除等关键动作，便于后续审计与问题定位。
 */
public final class Logs {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + "/campusloop.log";

    private static volatile boolean configured;

    private Logs() {
    }

    public static Logger getLogger(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }

    private static void configure() {
        if (configured) {
            return;
        }
        synchronized (Logs.class) {
            if (configured) {
                return;
            }
            Logger root = Logger.getLogger("");
            root.setLevel(Level.INFO);
            Formatter formatter = new SimpleFormatter();
            // 先移除 JDK 默认 ConsoleHandler，避免同一条日志被打印两次
            for (java.util.logging.Handler handler : root.getHandlers()) {
                root.removeHandler(handler);
            }
            ConsoleHandler console = new ConsoleHandler();
            console.setLevel(Level.INFO);
            console.setFormatter(formatter);
            root.addHandler(console);
            try {
                Files.createDirectories(Paths.get(LOG_DIR));
                FileHandler fileHandler = new FileHandler(LOG_FILE, true);
                fileHandler.setLevel(Level.INFO);
                fileHandler.setFormatter(formatter);
                root.addHandler(fileHandler);
            } catch (IOException e) {
                // 日志文件不可写（如只读目录）时降级为仅控制台输出，不影响主流程
                root.warning("日志文件初始化失败，仅输出到控制台：" + e.getMessage());
            }
            configured = true;
        }
    }

    private static final class SimpleFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%1$tF %1$tT %2$-7s %3$s - %4$s%n",
                    record.getMillis(),
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    formatMessage(record));
        }
    }
}
