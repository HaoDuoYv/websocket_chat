package com.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件上传配置属性类
 * 参考苍穹外卖项目实现
 */
@Component
@ConfigurationProperties(prefix = "local")
@Data
public class LocalProperties {

    /**
     * 本地存储路径（相对于项目根目录）
     */
    private String localUrl;

    /**
     * Web访问URL前缀
     */
    private String webUrl;
}
