package com.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/apps")
@CrossOrigin(origins = "*")
public class AppController {

    private static final Logger log = LoggerFactory.getLogger(AppController.class);

    @Value("${app.config-path:../config.js}")
    private String configPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> getApps() {
        try {
            List<Map<String, Object>> apps = parseProjectCards(configPath);
            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            log.error("读取应用配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "读取应用配置失败: " + e.getMessage()));
        }
    }

    private List<Map<String, Object>> parseProjectCards(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("配置文件不存在: " + path.toAbsolutePath());
        }

        String content = Files.readString(path);

        // 定位 projectcards 数组
        int start = content.indexOf("projectcards");
        if (start == -1) {
            throw new RuntimeException("无法在配置文件中找到 projectcards");
        }

        int bracketStart = content.indexOf('[', start);
        if (bracketStart == -1) {
            throw new RuntimeException("projectcards 格式错误：未找到数组起始");
        }

        // 通过括号匹配找到数组结束位置，同时处理字符串内的括号
        int depth = 1;
        int pos = bracketStart + 1;
        boolean inString = false;
        char stringChar = 0;

        while (depth > 0 && pos < content.length()) {
            char c = content.charAt(pos);
            if (inString) {
                if (c == stringChar && content.charAt(pos - 1) != '\\') {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringChar = c;
                } else if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                }
            }
            pos++;
        }

        String arrayContent = content.substring(bracketStart + 1, pos - 1);

        // 将 JS 对象字面量转换为有效 JSON
        // 1. 为未加引号的键添加双引号（仅在 { 或 , 之后出现的键）
        String jsonCompatible = arrayContent.replaceAll("([\\{,]\\s*)(\\w+)\\s*:", "$1\"$2\":");
        // 2. 移除尾随逗号（} 或 ] 前的逗号）
        jsonCompatible = jsonCompatible.replaceAll(",\\s*([}\\]])", "$1");
        jsonCompatible = jsonCompatible.replaceAll(",\\s*$", "");

        String jsonArray = "[" + jsonCompatible + "]";
        jsonArray = jsonArray.replaceAll(",\\s*([}\\]])", "$1");

        List<Map<String, Object>> cards = objectMapper.readValue(jsonArray, new TypeReference<List<Map<String, Object>>>() {});

        // 为每个项目添加 id 和额外字段
        for (Map<String, Object> card : cards) {
            String title = (String) card.getOrDefault("title", "");
            card.put("id", generateId(title));
            card.putIfAbsent("techStack", "");
            card.putIfAbsent("usageScenario", "");
            card.putIfAbsent("description", card.getOrDefault("text", ""));
            String url = (String) card.getOrDefault("url", "");
            card.putIfAbsent("status", (url != null && !url.isEmpty()) ? "active" : "planned");
        }

        return cards;
    }

    /**
     * 基于 title 生成确定性 id
     */
    private String generateId(String title) {
        return String.valueOf(Math.abs((long) title.hashCode()));
    }
}
