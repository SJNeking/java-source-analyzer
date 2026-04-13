package cn.dolphinmind.glossary.java.analyze.repository.impl;

import cn.dolphinmind.glossary.java.analyze.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 简单的数据库连接助手
 * 
 * 在没有引入连接池（如 HikariCP）之前，使用此工具类获取连接。
 */
public class DatabaseHelper {

    private static final Logger logger = Logger.getLogger(DatabaseHelper.class.getName());
    private static final AppConfig config = AppConfig.getInstance();

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                config.getDatabaseUrl(),
                config.getDatabaseUser(),
                config.getDatabasePassword()
            );
        } catch (SQLException e) {
            logger.warning("Failed to get DB connection: " + e.getMessage());
            throw e;
        }
    }
}
