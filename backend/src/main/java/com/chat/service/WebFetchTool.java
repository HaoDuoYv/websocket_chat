package com.chat.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebFetchTool {

    private static final Logger log = LoggerFactory.getLogger(WebFetchTool.class);
    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final int TIMEOUT_MS = 10000;

    public String execute(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "错误：未提供URL";
        }
        url = url.trim();
        // 如果 URL 不以 http 开头，补上 https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            log.info("WebFetch: 正在抓取 {}", url);
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .get();

            String title = doc.title();
            String content = extractContent(doc);

            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH) + "...(内容已截断)";
            }

            String result = "标题: " + title + "\n\n" + content;
            log.info("WebFetch: 成功抓取 {}, 返回 {} 字符", url, result.length());
            return result;
        } catch (Exception e) {
            log.warn("WebFetch failed for {}: {}", url, e.getMessage());
            return "抓取失败 (" + url + "): " + e.getMessage();
        }
    }

    private String extractContent(Document doc) {
        // 移除噪音标签
        doc.select("script, style, nav, footer, header, aside, form, iframe, noscript, svg").remove();

        // 优先提取 article 或 main
        Element main = doc.selectFirst("article");
        if (main == null) main = doc.selectFirst("main");
        if (main == null) main = doc.body();

        if (main == null) return "无法提取页面内容";

        // 提取文本，保留段落结构
        StringBuilder sb = new StringBuilder();
        for (Element el : main.select("p, h1, h2, h3, h4, h5, h6, li, td, th, blockquote, pre")) {
            String text = el.text().trim();
            if (!text.isEmpty()) {
                sb.append(text).append("\n");
            }
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) {
            result = main.text().trim();
        }
        return result;
    }

    public static String getToolDefinition() {
        return """
        {
          "type": "function",
          "function": {
            "name": "web_fetch",
            "description": "获取网页内容并提取正文。当用户发送了URL链接、询问某个网页的内容、或需要查询实时网络信息（天气、新闻、股票等）时，必须调用此工具。",
            "parameters": {
              "type": "object",
              "properties": {
                "url": {
                  "type": "string",
                  "description": "要获取的网页URL地址"
                }
              },
              "required": ["url"]
            }
          }
        }
        """;
    }
}
