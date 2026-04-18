package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 管理员配置类
 */
@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {

    /**
     * 允许访问管理后台的IP白名单
     */
    private List<String> allowedIps;

    private String username = "admin";

    private String passwordHash="kun666777@";

    /**
     * 日志文件路径
     */
    private String logPath = "logs";

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
