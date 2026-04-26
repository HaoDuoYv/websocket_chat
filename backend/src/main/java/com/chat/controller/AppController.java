package com.chat.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/apps")
@CrossOrigin(origins = "*")
public class AppController {

    private static final Logger log = LoggerFactory.getLogger(AppController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> getApps() {
        try (InputStream is = getClass().getResourceAsStream("/apps.json")) {
            if (is == null) {
                log.error("apps.json 未在 classpath 中找到");
                return ResponseEntity.internalServerError().body(Map.of("message", "应用配置文件未找到"));
            }
            List<Map<String, Object>> apps = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> app : apps) {
                String title = (String) app.getOrDefault("title", "");
                app.putIfAbsent("id", generateId(title));
                app.putIfAbsent("techStack", "");
                app.putIfAbsent("usageScenario", "");
                app.putIfAbsent("description", app.getOrDefault("text", ""));
                String url = (String) app.getOrDefault("url", "");
                app.putIfAbsent("status", (url != null && !url.isEmpty()) ? "active" : "planned");
            }
            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            log.error("读取应用配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "读取应用配置失败: " + e.getMessage()));
        }
    }

    private String generateId(String title) {
        return String.valueOf(Math.abs((long) title.hashCode()));
    }
}
