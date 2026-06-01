package com.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
public class ToolCallingService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebFetchTool webFetchTool;

    @Autowired
    private WebSearchTool webSearchTool;

    public static final int MAX_TOOL_ROUNDS = 5;

    public List<String> getToolDefinitions() {
        return List.of(WebSearchTool.getToolDefinition(), WebFetchTool.getToolDefinition());
    }

    public String executeTool(String toolName, String argsJson) {
        try {
            log.info("执行工具 {}, args={}", toolName, argsJson);
            JsonNode args = objectMapper.readTree(argsJson);
            String result = switch (toolName) {
                case "web_search" -> {
                    String query = "";
                    if (args.has("query")) {
                        query = args.get("query").asText();
                    } else if (args.isTextual()) {
                        query = args.asText();
                    }
                    yield webSearchTool.execute(query);
                }
                case "web_fetch" -> {
                    String url = "";
                    if (args.has("url")) {
                        url = args.get("url").asText();
                    } else if (args.isTextual()) {
                        // 兼容参数直接是 URL 字符串的情况
                        url = args.asText();
                    }
                    yield webFetchTool.execute(url);
                }
                default -> "未知工具: " + toolName;
            };
            log.info("工具 {} 执行完成, 返回 {} 字符", toolName, result.length());
            return result;
        } catch (Exception e) {
            log.error("工具执行失败 {}: {}", toolName, e.getMessage());
            // 兜底：尝试直接把 argsJson 当 URL 用
            if ("web_fetch".equals(toolName) && argsJson != null && argsJson.startsWith("http")) {
                return webFetchTool.execute(argsJson.replaceAll("\"", ""));
            }
            return "工具执行失败: " + e.getMessage();
        }
    }
}
