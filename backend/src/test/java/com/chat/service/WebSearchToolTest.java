package com.chat.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    private final WebSearchTool tool = new WebSearchTool();

    @Test
    void searchWithValidQuery() {
        String result = tool.execute("weather today");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.startsWith("搜索失败"), "搜索不应失败: " + result);
        assertFalse(result.startsWith("错误"), "不应报错: " + result);
        System.out.println("=== 搜索结果 ===\n" + result);
    }

    @Test
    void searchWithChineseQuery() {
        String result = tool.execute("今天天气");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("=== 中文搜索结果 ===\n" + result);
    }

    @Test
    void searchWithEmptyQuery() {
        String result = tool.execute("");
        assertTrue(result.startsWith("错误"));
    }

    @Test
    void searchWithNullQuery() {
        String result = tool.execute(null);
        assertTrue(result.startsWith("错误"));
    }

    @Test
    void toolDefinitionIsValid() {
        String def = WebSearchTool.getToolDefinition();
        assertNotNull(def);
        assertTrue(def.contains("web_search"));
        assertTrue(def.contains("query"));
    }
}
