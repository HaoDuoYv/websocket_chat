package com.chat.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final int MAX_RESULTS = 5;
    private static final int TIMEOUT_MS = 10000;
    private static final int MAX_CONTENT_LENGTH = 6000;

    public String execute(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "错误：未提供搜索关键词";
        }
        query = query.trim();
        try {
            log.info("WebSearch: 正在搜索 {}", query);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

            Document doc = Jsoup.connect(searchUrl)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .followRedirects(true)
                    .get();

            Elements results = doc.select(".result");
            if (results.isEmpty()) {
                log.info("WebSearch: 搜索 {} 无结果", query);
                return "未找到关于 \"" + query + "\" 的相关结果";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("搜索结果: ").append(query).append("\n\n");

            int count = 0;
            for (Element result : results) {
                if (count >= MAX_RESULTS) break;

                Element titleEl = result.selectFirst(".result__a");
                Element snippetEl = result.selectFirst(".result__snippet");
                Element urlEl = result.selectFirst(".result__url");

                if (titleEl == null) continue;

                String title = titleEl.text().trim();
                String snippet = snippetEl != null ? snippetEl.text().trim() : "";
                String link = urlEl != null ? urlEl.text().trim() : "";

                count++;
                sb.append(count).append(". ").append(title).append("\n");
                if (!snippet.isEmpty()) {
                    sb.append("   ").append(snippet).append("\n");
                }
                if (!link.isEmpty()) {
                    sb.append("   ").append(link).append("\n");
                }
                sb.append("\n");
            }

            String result = sb.toString().trim();
            if (result.length() > MAX_CONTENT_LENGTH) {
                result = result.substring(0, MAX_CONTENT_LENGTH) + "...(结果已截断)";
            }

            log.info("WebSearch: 搜索 {} 完成, 返回 {} 条结果, {} 字符", query, count, result.length());
            return result;
        } catch (Exception e) {
            log.warn("WebSearch failed for {}: {}", query, e.getMessage());
            return "搜索失败: " + e.getMessage();
        }
    }

    public static String getToolDefinition() {
        return """
        {
          "type": "function",
          "function": {
            "name": "web_search",
            "description": "搜索引擎，用于查找实时网络信息。当用户询问天气、新闻、股价、赛事结果、百科知识、最新资讯等需要联网查询的问题时，必须调用此工具。",
            "parameters": {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "搜索关键词，应简洁明确"
                }
              },
              "required": ["query"]
            }
          }
        }
        """;
    }
}
