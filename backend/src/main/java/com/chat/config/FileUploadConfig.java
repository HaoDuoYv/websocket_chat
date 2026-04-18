package com.chat.config;

import com.chat.properties.LocalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.File;

/**
 * 文件上传配置类
 * 参考苍穹外卖项目 WebMvcConfiguration 实现
 * 继承 WebMvcConfigurationSupport 配置静态资源映射
 */
@Configuration
@Slf4j
public class FileUploadConfig extends WebMvcConfigurationSupport {

    @Autowired
    private LocalProperties localProperties;

    /**
     * 设置静态资源映射
     * 将 /files/** 映射到本地文件系统路径
     *
     * @param registry 资源处理器注册表
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始映射静态资源");

        // 获取本地存储路径（相对路径，如 ../uploads）
        String localUrl = localProperties.getLocalUrl();
        log.info("配置的本地路径：{}", localUrl);

        // 构建绝对路径
        // 方法：基于当前工作目录 + 配置的相对路径
        String basePath = System.getProperty("user.dir");
        File uploadDir = new File(basePath, localUrl);
        String absolutePath = uploadDir.getAbsolutePath();

        log.info("工作目录：{}", basePath);
        log.info("上传目录绝对路径：{}", absolutePath);

        // 确保目录存在
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            log.info("创建上传目录：{}", created ? "成功" : "失败");
        }

        // 配置静态资源映射
        // file: 协议后接绝对路径，Windows 下 Spring 会自动处理
        String resourceLocation = "file:" + absolutePath + "/";
        String handlerPattern = localProperties.getWebUrl() + "/**";

        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(resourceLocation);

        log.info("静态资源映射：{} -> {}", handlerPattern, resourceLocation);
    }
}
