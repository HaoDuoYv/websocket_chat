package com.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

@Component
public class DatabaseMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (!columnExists(conn, "users", "token_version")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0");
                    logger.info("已添加 users.token_version 列");
                }
            }
        } catch (Exception e) {
            logger.warn("数据库迁移检查失败: {}", e.getMessage());
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
