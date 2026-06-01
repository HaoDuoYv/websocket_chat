package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.AiConversation;
import com.chat.entity.AiMessage;
import com.chat.repository.AiMessageRepository;
import com.chat.utils.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.springframework.http.client.reactive.JdkClientHttpConnector;

import java.net.http.HttpClient;
import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
public class AiChatService {

    @Autowired
    private AiMessageRepository aiMessageRepository;

    @Autowired
    private com.chat.repository.MessageRepository messageRepository;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private UserService userService;

    @Autowired
    private ToolCallingService toolCallingService;

    @Autowired
    private WebSearchTool webSearchTool;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    private static final int SUMMARY_THRESHOLD = 20;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用 JDK 内置 HttpClient 构建 WebClient，避免 Netty SslHandler 与阿里云等服务器的 TLS 握手失败问题。
     * JDK 原生 TLS 栈对阿里云 DashScope 等服务的兼容性更好。
     */
    private WebClient.Builder webClientBuilder() {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(jdkHttpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024));
    }

    public AiMessage saveUserMessage(Long conversationId, String content) {
        return saveUserMessage(conversationId, content, java.util.Collections.emptyList());
    }

    public AiMessage saveUserMessage(Long conversationId, String content, List<Map<String, String>> attachments) {
        AiMessage message = new AiMessage();
        message.setId(idGenerator.nextId());
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(buildStoredUserContent(content, attachments));
        message.setCreatedAt(System.currentTimeMillis());
        return aiMessageRepository.save(message);
    }

    private String buildStoredUserContent(String content, List<Map<String, String>> attachments) {
        StringBuilder sb = new StringBuilder(content == null ? "" : content);
        if (attachments != null) {
            for (Map<String, String> att : attachments) {
                String kind = att.getOrDefault("kind", "");
                String name = att.getOrDefault("name", "");
                if (sb.length() > 0) sb.append('\n');
                if ("image".equals(kind)) {
                    sb.append("[图片: ").append(name).append("]");
                } else {
                    sb.append("[文件: ").append(name).append("]");
                }
            }
        }
        return sb.toString();
    }

    public AiMessage saveAssistantMessage(Long conversationId, String content, Integer tokenCount) {
        AiMessage message = new AiMessage();
        message.setId(idGenerator.nextId());
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(content);
        message.setTokenCount(tokenCount);
        message.setCreatedAt(System.currentTimeMillis());
        return aiMessageRepository.save(message);
    }

    // 原始方法（无 tool calling）
    public void streamChat(Long assistantId, Long conversationId, String userContent, Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError) {
        streamChat(assistantId, conversationId, userContent, false, java.util.Collections.emptyList(), onToken, onComplete, onError, null, null);
    }

    // 带 tool calling 的方法
    public void streamChat(Long assistantId, Long conversationId, String userContent, boolean webSearch,
                           List<Map<String, String>> attachments,
                           Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError,
                           Consumer<Map<String, String>> onToolCall, Consumer<Map<String, String>> onToolResult) {
        try {
            Optional<AiAssistant> assistantOpt = aiAssistantService.getAssistantById(assistantId);
            if (assistantOpt.isEmpty()) {
                onError.accept("AI助手不存在");
                return;
            }
            AiAssistant assistant = assistantOpt.get();

            saveUserMessage(conversationId, userContent, attachments);
            aiConversationService.incrementMessageCount(conversationId);

            List<Message> messages = buildContext(assistant, conversationId);

            String baseUrl = assistant.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            // 构建 chat completions 端点URL
            String chatUrl;
            if (baseUrl.endsWith("/chat/completions")) {
                chatUrl = baseUrl;
            } else if (baseUrl.contains("/chat/completions?")) {
                chatUrl = baseUrl;
            } else {
                chatUrl = baseUrl + "/chat/completions";
            }

            boolean isGlm = baseUrl.contains("open.bigmodel.cn");

            List<String> toolDefs = toolCallingService.getToolDefinitions();

            // 注入工具可用性提示到系统消息
            if (!toolDefs.isEmpty()) {
                int insertIndex = Math.min(1, messages.size());
                messages.add(insertIndex, new SystemMessage(buildToolAvailabilityPrompt(toolDefs, webSearch)));
            }

            if (isGlm) {
                toolCallingGlm(assistant, conversationId, messages, toolDefs, webSearch, attachments, onToken, onComplete, onError, onToolCall, onToolResult);
            } else {
                toolCallingOpenAi(assistant, conversationId, messages, chatUrl, toolDefs, attachments, onToken, onComplete, onError, onToolCall, onToolResult);
            }

        } catch (Exception e) {
            onError.accept("AI调用失败: " + e.getMessage());
        }
    }

    // ==================== OpenAI 路径 (原始 WebClient, agent 式 function calling) ====================

    private void toolCallingOpenAi(AiAssistant assistant, Long conversationId, List<Message> messages,
                                   String chatUrl, List<String> toolDefs,
                                   List<Map<String, String>> attachments,
                                   Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError,
                                   Consumer<Map<String, String>> onToolCall, Consumer<Map<String, String>> onToolResult) {
        try {
            double temp = assistant.getTemperature() != null ? assistant.getTemperature() : 0.7;

            WebClient webClient = webClientBuilder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .build();

            // 将 Spring AI Message 转为 JSON messages 数组
            List<ObjectNode> agentMessages = new ArrayList<>();
            for (Message msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                String text = msg.getText();
                if (msg instanceof SystemMessage) {
                    msgNode.put("role", "system");
                } else if (msg instanceof UserMessage) {
                    msgNode.put("role", "user");
                } else if (msg instanceof AssistantMessage) {
                    msgNode.put("role", "assistant");
                } else {
                    msgNode.put("role", "user");
                }
                msgNode.put("content", text != null ? text : "");
                agentMessages.add(msgNode);
            }

            // 替换最后一条 user 消息为 multimodal content 数组（仅当有附件）
            if (attachments != null && !attachments.isEmpty()) {
                for (int i = agentMessages.size() - 1; i >= 0; i--) {
                    ObjectNode m = agentMessages.get(i);
                    if (m.has("role") && "user".equals(m.get("role").asText())) {
                        String origText = m.has("content") ? m.get("content").asText() : "";
                        ArrayNode mm = buildUserMessageContent(origText, attachments);
                        if (mm != null) {
                            m.remove("content");
                            m.set("content", mm);
                        }
                        break;
                    }
                }
            }

            // 构建 tools 数组（OpenAI function calling 格式）
            ArrayNode toolsArray = objectMapper.createArrayNode();
            for (String toolDefJson : toolDefs) {
                toolsArray.add(objectMapper.readTree(toolDefJson));
            }
            String toolsJson = objectMapper.writeValueAsString(toolsArray);
            log.info("OpenAI agent: tools={}", toolsJson);

            // Agent 循环：最多 MAX_TOOL_ROUNDS 轮
            for (int round = 0; round <= ToolCallingService.MAX_TOOL_ROUNDS; round++) {
                // 构建请求体（非流式，用于检测 tool_calls）
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", assistant.getModel());
                requestBody.put("temperature", temp);
                requestBody.put("stream", false);

                ArrayNode msgsArray = requestBody.putArray("messages");
                for (ObjectNode msg : agentMessages) {
                    msgsArray.add(msg);
                }
                requestBody.set("tools", toolsArray);

                String bodyJson = objectMapper.writeValueAsString(requestBody);
                log.info("OpenAI agent round {}: 发送请求, messages={}, model={}", round, agentMessages.size(), assistant.getModel());
                if (round > 0) {
                    // 打印 tool 相关消息，便于调试 400 错误
                    for (int i = 0; i < agentMessages.size(); i++) {
                        ObjectNode m = agentMessages.get(i);
                        String role = m.has("role") ? m.get("role").asText() : "?";
                        boolean hasToolCalls = m.has("tool_calls") && m.get("tool_calls").isArray() && m.get("tool_calls").size() > 0;
                        boolean hasToolCallId = m.has("tool_call_id");
                        String contentPreview = m.has("content") && !m.get("content").isNull() ? m.get("content").asText().substring(0, Math.min(50, m.get("content").asText().length())) : "null";
                        log.info("  msg[{}] role={} content='{}' hasToolCalls={} hasToolCallId={}", i, role, contentPreview, hasToolCalls, hasToolCallId);
                    }
                }

                // 非流式调用
                String responseJson;
                try {
                    responseJson = webClient.post()
                            .uri(chatUrl)
                            .bodyValue(bodyJson)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                    String errBody = e.getResponseBodyAsString();
                    String statusText = e.getStatusText() != null ? e.getStatusText() : "";
                    String headers = e.getHeaders() != null ? e.getHeaders().toString() : "";
                    log.error("API返回错误 {} {}: body=[{}] headers=[{}] requestUrl={}", e.getStatusCode(), statusText, errBody, headers, chatUrl);
                    String detail = !errBody.isEmpty() ? errBody
                            : "status=" + e.getStatusCode().value() + " " + statusText + ", url=" + chatUrl;
                    onError.accept("API调用失败(" + e.getStatusCode().value() + "): " + detail);
                    return;
                } catch (Exception e) {
                    String errMsg = e.getMessage();
                    log.error("API调用异常: {}", errMsg, e);
                    onError.accept("API调用失败: " + errMsg);
                    return;
                }

                if (responseJson == null) {
                    onError.accept("API返回为空");
                    return;
                }

                log.info("OpenAI agent round {}: 响应前200字符: {}", round, responseJson.substring(0, Math.min(200, responseJson.length())));

                JsonNode node = objectMapper.readTree(responseJson);
                JsonNode choices = node.get("choices");
                if (choices == null || !choices.isArray() || choices.isEmpty()) {
                    onError.accept("API响应格式错误: " + responseJson.substring(0, Math.min(500, responseJson.length())));
                    return;
                }

                JsonNode choice = choices.get(0);
                JsonNode messageNode = choice.get("message");
                if (messageNode == null) {
                    onError.accept("API响应中无message");
                    return;
                }

                String assistantContent = messageNode.has("content") && !messageNode.get("content").isNull()
                        ? messageNode.get("content").asText() : "";

                // 检查是否有 tool_calls
                JsonNode toolCallsNode = messageNode.get("tool_calls");
                if (toolCallsNode != null && toolCallsNode.isArray() && toolCallsNode.size() > 0) {
                    log.info("OpenAI agent round {}: 检测到 {} 个工具调用", round, toolCallsNode.size());

                    // 将 assistant message（含 tool_calls）加入历史
                    // 注意：有 tool_calls 时 content 必须为 null，不能是空字符串
                    ObjectNode assistantMsg = objectMapper.createObjectNode();
                    assistantMsg.put("role", "assistant");
                    if (assistantContent != null && !assistantContent.isEmpty()) {
                        assistantMsg.put("content", assistantContent);
                    } else {
                        assistantMsg.putNull("content");
                    }
                    assistantMsg.set("tool_calls", toolCallsNode);
                    // DeepSeek 思考模式要求 reasoning_content 必须原样传回
                    JsonNode reasoningNode = messageNode.get("reasoning_content");
                    if (reasoningNode != null && !reasoningNode.isNull()) {
                        assistantMsg.set("reasoning_content", reasoningNode);
                    }
                    agentMessages.add(assistantMsg);

                    // 执行每个工具调用
                    for (JsonNode tc : toolCallsNode) {
                        String callId = tc.has("id") ? tc.get("id").asText() : "call_" + System.currentTimeMillis();
                        JsonNode funcNode = tc.get("function");
                        String toolName = funcNode != null && funcNode.has("name") ? funcNode.get("name").asText() : "unknown";
                        String args = funcNode != null && funcNode.has("arguments") ? funcNode.get("arguments").asText() : "{}";

                        // 通知前端：工具调用开始
                        if (onToolCall != null) {
                            onToolCall.accept(Map.of(
                                    "callId", callId,
                                    "toolName", toolName,
                                    "args", args
                            ));
                        }

                        log.info("OpenAI agent: 执行工具 {} with args {}", toolName, args);

                        // 执行工具
                        String result = toolCallingService.executeTool(toolName, args);
                        if (result == null || result.isEmpty()) {
                            result = "工具未返回任何内容";
                        }

                        // 通知前端：工具调用完成
                        if (onToolResult != null) {
                            onToolResult.accept(Map.of(
                                    "callId", callId,
                                    "toolName", toolName,
                                    "result", result.length() > 500 ? result.substring(0, 500) + "..." : result
                            ));
                        }

                        log.info("OpenAI agent: 工具 {} 返回 {} 字符", toolName, result.length());

                        // 添加 tool result 到消息历史
                        ObjectNode toolResultMsg = objectMapper.createObjectNode();
                        toolResultMsg.put("role", "tool");
                        toolResultMsg.put("tool_call_id", callId);
                        toolResultMsg.put("content", result);
                        agentMessages.add(toolResultMsg);
                    }
                    // 继续循环，让模型基于工具结果生成回复
                } else {
                    // 没有 tool_calls，这是最终回复
                    assistantContent = cleanToolArtifacts(assistantContent);
                    log.info("OpenAI agent: 最终回复 ({}字符)", assistantContent.length());

                    // 分批发送，每批 20 字符，间隔 10ms，避免 WebSocket 缓冲溢出
                    if (!assistantContent.isEmpty()) {
                        int chunkSize = 20;
                        for (int i = 0; i < assistantContent.length(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, assistantContent.length());
                            onToken.accept(assistantContent.substring(i, end));
                            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                        }
                    }

                    if (conversationId != null) {
                        aiConversationService.incrementMessageCount(conversationId);
                        checkAndSummarize(conversationId, assistant);
                    }
                    onComplete.accept(assistantContent);
                    return;
                }
            }

            onError.accept("工具调用轮次超限（" + ToolCallingService.MAX_TOOL_ROUNDS + "轮）");
        } catch (Exception e) {
            log.error("OpenAI agent 失败: {}", e.getMessage(), e);
            onError.accept("AI调用失败: " + e.getMessage());
        }
    }

    // ==================== GLM 路径 (内置 web_search 工具) ====================

    private void toolCallingGlm(AiAssistant assistant, Long conversationId, List<Message> messages,
                                List<String> toolDefs, boolean webSearch,
                                List<Map<String, String>> attachments,
                                Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError,
                                Consumer<Map<String, String>> onToolCall, Consumer<Map<String, String>> onToolResult) {
        try {
            String url = assistant.getBaseUrl();
            if (!url.endsWith("/chat/completions")) {
                url = url + "/chat/completions";
            }

            double temp = assistant.getTemperature() != null ? assistant.getTemperature() : 0.7;
            temp = Math.min(temp, 1.0);
            temp = Math.max(temp, 0.0);

            // 构建消息列表（群聊模式 conversationId=null 时直接转换传入的 messages）
            List<ObjectNode> glmMessages;
            if (conversationId != null) {
                glmMessages = buildGlmMessages(assistant, conversationId);
            } else {
                glmMessages = new ArrayList<>();
                for (Message msg : messages) {
                    ObjectNode msgNode = objectMapper.createObjectNode();
                    String text = msg.getText();
                    if (msg instanceof SystemMessage) {
                        msgNode.put("role", "system");
                    } else if (msg instanceof UserMessage) {
                        msgNode.put("role", "user");
                    } else if (msg instanceof AssistantMessage) {
                        msgNode.put("role", "assistant");
                    } else {
                        msgNode.put("role", "user");
                    }
                    msgNode.put("content", text != null ? text : "");
                    glmMessages.add(msgNode);
                }
            }

            if (attachments != null && !attachments.isEmpty()) {
                for (int i = glmMessages.size() - 1; i >= 0; i--) {
                    ObjectNode m = glmMessages.get(i);
                    if (m.has("role") && "user".equals(m.get("role").asText())) {
                        String origText = m.has("content") ? m.get("content").asText() : "";
                        ArrayNode mm = buildUserMessageContent(origText, attachments);
                        if (mm != null) {
                            m.remove("content");
                            m.set("content", mm);
                        }
                        break;
                    }
                }
            }

            // 注入工具可用性提示
            if (!toolDefs.isEmpty()) {
                ObjectNode toolPromptMsg = objectMapper.createObjectNode();
                toolPromptMsg.put("role", "system");
                toolPromptMsg.put("content", buildToolAvailabilityPrompt(toolDefs, webSearch));
                int insertIndex = Math.min(1, glmMessages.size());
                glmMessages.add(insertIndex, toolPromptMsg);
            }

            WebClient webClient = webClientBuilder()
                    .baseUrl(url)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .build();

            // 第一次调用：启用 GLM 内置 web_search 工具
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", assistant.getModel());
            requestBody.put("temperature", temp);
            requestBody.put("stream", false);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ObjectNode msg : glmMessages) {
                messagesArray.add(msg);
            }

            // 使用 GLM 内置 WebSearchToolSchema
            ArrayNode toolsArray = requestBody.putArray("tools");
            ObjectNode webSearchToolSchema = objectMapper.createObjectNode();
            webSearchToolSchema.put("type", "web_search");
            ObjectNode webSearchConfig = webSearchToolSchema.putObject("web_search");
            webSearchConfig.put("enable", true);
            toolsArray.add(webSearchToolSchema);

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("GLM请求(带web_search): {}", bodyJson);

            String responseJson;
            try {
                responseJson = webClient.post()
                        .bodyValue(bodyJson)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                String errBody = e.getResponseBodyAsString();
                String statusText = e.getStatusText() != null ? e.getStatusText() : "";
                log.error("GLM API返回错误 {} {}: body=[{}] url={}", e.getStatusCode(), statusText, errBody, url);
                String detail = !errBody.isEmpty() ? errBody
                        : "status=" + e.getStatusCode().value() + " " + statusText + ", url=" + url;
                onError.accept("GLM API调用失败(" + e.getStatusCode().value() + "): " + detail);
                return;
            }

            if (responseJson == null) {
                onError.accept("GLM返回为空");
                return;
            }

            JsonNode node = objectMapper.readTree(responseJson);
            JsonNode choices = node.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                onError.accept("GLM响应格式错误: " + responseJson.substring(0, Math.min(500, responseJson.length())));
                return;
            }

            JsonNode choice = choices.get(0);
            JsonNode message = choice.get("message");

            if (message == null) {
                onError.accept("GLM响应中无message");
                return;
            }

            String content = message.has("content") ? message.get("content").asText() : "";

            // 检查是否有 web_search 结果
            JsonNode webSearchResults = node.get("web_search");
            if (webSearchResults != null && webSearchResults.isArray() && webSearchResults.size() > 0) {
                // 通知前端：正在搜索
                if (onToolCall != null) {
                    onToolCall.accept(Map.of(
                            "callId", "glm_search_" + System.currentTimeMillis(),
                            "toolName", "web_search",
                            "args", "{\"engine\":\"search_std\"}"
                    ));
                }

                // 构建搜索结果摘要
                StringBuilder searchSummary = new StringBuilder();
                for (JsonNode result : webSearchResults) {
                    String title = result.has("title") ? result.get("title").asText() : "";
                    String resultContent = result.has("content") ? result.get("content").asText() : "";
                    String link = result.has("link") ? result.get("link").asText() : "";
                    if (!title.isEmpty()) {
                        searchSummary.append("[").append(title).append("](").append(link).append(")\n");
                    }
                    if (!resultContent.isEmpty()) {
                        searchSummary.append(resultContent).append("\n\n");
                    }
                }

                // 通知前端：搜索完成
                if (onToolResult != null) {
                    String summary = searchSummary.toString();
                    onToolResult.accept(Map.of(
                            "callId", "glm_search_" + System.currentTimeMillis(),
                            "toolName", "web_search",
                            "result", summary.length() > 500 ? summary.substring(0, 500) + "..." : summary
                    ));
                }

                // Fallback: 如果智谱 web_search 无结果，尝试 DuckDuckGo
                if (searchSummary.length() == 0) {
                    String ddgQuery = "";
                    for (int i = glmMessages.size() - 1; i >= 0; i--) {
                        if ("user".equals(glmMessages.get(i).get("role").asText())) {
                            ddgQuery = glmMessages.get(i).get("content").asText();
                            break;
                        }
                    }
                    String ddgResult = webSearchTool.execute(ddgQuery);
                    if (!ddgResult.startsWith("搜索失败") && !ddgResult.startsWith("未找到")) {
                        searchSummary.append(ddgResult);
                        if (onToolCall != null) {
                            onToolCall.accept(Map.of(
                                    "callId", "ddg_search_" + System.currentTimeMillis(),
                                    "toolName", "web_search",
                                    "args", "{\"engine\":\"duckduckgo\"}"
                            ));
                        }
                        if (onToolResult != null) {
                            onToolResult.accept(Map.of(
                                    "callId", "ddg_search_" + System.currentTimeMillis(),
                                    "result", ddgResult.length() > 500 ? ddgResult.substring(0, 500) + "..." : ddgResult
                            ));
                        }
                    }
                }

                // 将搜索结果注入上下文，重新调用 LLM 生成最终回复
                ObjectNode searchContextMsg = objectMapper.createObjectNode();
                searchContextMsg.put("role", "system");
                searchContextMsg.put("content", "以下是网络搜索结果，请基于这些信息回答用户的问题：\n\n" + searchSummary);
                glmMessages.add(searchContextMsg);

                // 第二次调用：基于搜索结果生成回复（流式）
                ObjectNode requestBody2 = objectMapper.createObjectNode();
                requestBody2.put("model", assistant.getModel());
                requestBody2.put("temperature", temp);
                requestBody2.put("stream", true);

                ArrayNode messagesArray2 = requestBody2.putArray("messages");
                for (ObjectNode msg : glmMessages) {
                    messagesArray2.add(msg);
                }

                String bodyJson2 = objectMapper.writeValueAsString(requestBody2);
                log.info("GLM请求(搜索后生成): {}", bodyJson2);

                StringBuilder fullResponse = new StringBuilder();

                webClient.post()
                        .bodyValue(bodyJson2)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .filter(line -> !line.isEmpty() && !line.equals("[DONE]"))
                        .subscribe(
                            line -> {
                                try {
                                    if (line.startsWith("data: ")) {
                                        line = line.substring(6);
                                    }
                                    if (line.equals("[DONE]")) return;

                                    JsonNode streamNode = objectMapper.readTree(line);
                                    JsonNode streamChoices = streamNode.get("choices");
                                    if (streamChoices != null && streamChoices.isArray() && streamChoices.size() > 0) {
                                        JsonNode delta = streamChoices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String token = delta.get("content").asText();
                                            if (token != null && !token.isEmpty()) {
                                                fullResponse.append(token);
                                                onToken.accept(token);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.debug("GLM流式解析跳过: {}", line);
                                }
                            },
                            error -> {
                                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                                    String errBody = wcre.getResponseBodyAsString();
                                    String statusText = wcre.getStatusText() != null ? wcre.getStatusText() : "";
                                    log.error("GLM流式 API返回错误 {} {}: body=[{}]", wcre.getStatusCode(), statusText, errBody);
                                    String detail = !errBody.isEmpty() ? errBody
                                            : "status=" + wcre.getStatusCode().value() + " " + statusText;
                                    onError.accept("GLM流式调用失败(" + wcre.getStatusCode().value() + "): " + detail);
                                } else {
                                    onError.accept("GLM流式调用失败: " + error.getMessage());
                                }
                            },
                            () -> {
                                String completeContent = cleanToolArtifacts(fullResponse.toString());
                                if (conversationId != null) {
                                    aiConversationService.incrementMessageCount(conversationId);
                                    checkAndSummarize(conversationId, assistant);
                                }
                                onComplete.accept(completeContent);
                            }
                        );
            } else {
                // 没有搜索结果，直接返回文本
                if (conversationId != null) {
                    aiConversationService.incrementMessageCount(conversationId);
                    checkAndSummarize(conversationId, assistant);
                }
                onComplete.accept(cleanToolArtifacts(content));
            }

        } catch (Exception e) {
            log.error("GLM调用失败: {}", e.getMessage(), e);
            onError.accept("GLM调用失败: " + e.getMessage());
        }
    }

    private List<ObjectNode> buildGlmMessages(AiAssistant assistant, Long conversationId) {
        List<ObjectNode> messages = new ArrayList<>();

        if (assistant.getSystemPrompt() != null && !assistant.getSystemPrompt().isEmpty()) {
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", assistant.getSystemPrompt());
            messages.add(sysMsg);
        }

        Optional<AiConversation> conversationOpt = aiConversationService.getConversationById(conversationId);
        if (conversationOpt.isPresent() && conversationOpt.get().getSummary() != null) {
            ObjectNode summaryMsg = objectMapper.createObjectNode();
            summaryMsg.put("role", "system");
            summaryMsg.put("content", "之前的对话摘要: " + conversationOpt.get().getSummary());
            messages.add(summaryMsg);
        }

        int maxContext = assistant.getMaxContext() != null ? assistant.getMaxContext() : 20;
        List<AiMessage> recentMessages = aiMessageRepository.findRecentByConversationId(conversationId, PageRequest.of(0, maxContext));
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            AiMessage msg = recentMessages.get(i);
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
            messages.add(msgNode);
        }

        return messages;
    }

    // ==================== 原始流式方法（保留兼容） ====================

    private void streamGlmChat(AiAssistant assistant, Long conversationId, String userContent, Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError) {
        try {
            String url = assistant.getBaseUrl();
            if (!url.endsWith("/chat/completions")) {
                url = url + "/chat/completions";
            }

            double temp = assistant.getTemperature() != null ? assistant.getTemperature() : 0.7;
            temp = Math.min(temp, 1.0);
            temp = Math.max(temp, 0.0);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.put("model", assistant.getModel());
            requestBody.put("temperature", temp);
            requestBody.put("stream", true);

            com.fasterxml.jackson.databind.node.ArrayNode messagesArray = requestBody.putArray("messages");

            if (assistant.getSystemPrompt() != null && !assistant.getSystemPrompt().isEmpty()) {
                ObjectNode sysMsg = messagesArray.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", assistant.getSystemPrompt());
            }

            Optional<AiConversation> conversationOpt = aiConversationService.getConversationById(conversationId);
            if (conversationOpt.isPresent() && conversationOpt.get().getSummary() != null) {
                ObjectNode summaryMsg = messagesArray.addObject();
                summaryMsg.put("role", "system");
                summaryMsg.put("content", "之前的对话摘要: " + conversationOpt.get().getSummary());
            }

            int maxContext = assistant.getMaxContext() != null ? assistant.getMaxContext() : 20;
            List<AiMessage> recentMessages = aiMessageRepository.findRecentByConversationId(conversationId, PageRequest.of(0, maxContext));
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                AiMessage msg = recentMessages.get(i);
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
            }

            ObjectNode userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            String bodyJson = mapper.writeValueAsString(requestBody);
            log.info("GLM请求: {}", bodyJson);

            WebClient webClient = webClientBuilder()
                    .baseUrl(url)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .build();

            StringBuilder fullResponse = new StringBuilder();

            webClient.post()
                    .bodyValue(bodyJson)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .filter(line -> !line.isEmpty() && !line.equals("[DONE]"))
                    .subscribe(
                        line -> {
                            try {
                                if (line.startsWith("data: ")) {
                                    line = line.substring(6);
                                }
                                if (line.equals("[DONE]")) return;

                                JsonNode node = objectMapper.readTree(line);
                                JsonNode choices = node.get("choices");
                                if (choices != null && choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String token = delta.get("content").asText();
                                        if (token != null && !token.isEmpty()) {
                                            fullResponse.append(token);
                                            onToken.accept(token);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("GLM流式解析跳过: {}", line);
                            }
                        },
                        error -> {
                            if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                                String errBody = wcre.getResponseBodyAsString();
                                String statusText = wcre.getStatusText() != null ? wcre.getStatusText() : "";
                                log.error("GLM API返回错误 {} {}: body=[{}]", wcre.getStatusCode(), statusText, errBody);
                                String detail = !errBody.isEmpty() ? errBody
                                        : "status=" + wcre.getStatusCode().value() + " " + statusText;
                                onError.accept("GLM API调用失败(" + wcre.getStatusCode().value() + "): " + detail);
                            } else {
                                String errorMsg = error.getMessage();
                                if (errorMsg != null && errorMsg.contains("400")) {
                                    onError.accept("GLM API调用失败(400)：请检查模型名称和temperature(0-1)。原始错误: " + errorMsg);
                                } else if (errorMsg != null && errorMsg.contains("401")) {
                                    onError.accept("GLM API认证失败(401)：请检查API Key。");
                                } else {
                                    onError.accept("GLM API调用失败: " + errorMsg);
                                }
                            }
                        },
                        () -> {
                            String completeContent = fullResponse.toString();
                            aiConversationService.incrementMessageCount(conversationId);
                            checkAndSummarize(conversationId, assistant);
                            onComplete.accept(completeContent);
                        }
                    );

        } catch (Exception e) {
            onError.accept("GLM调用失败: " + e.getMessage());
        }
    }

    private void streamOpenAiChat(AiAssistant assistant, Long conversationId, List<Message> messages, String baseUrl, Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError) {
        try {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(assistant.getApiKey())
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(assistant.getModel())
                    .temperature(assistant.getTemperature() != null ? assistant.getTemperature() : 0.7)
                    .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();

            Prompt prompt = new Prompt(messages, options);
            StringBuilder fullResponse = new StringBuilder();

            Flux<org.springframework.ai.chat.model.ChatResponse> responseFlux = chatModel.stream(prompt);

            responseFlux.subscribe(
                chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        String token = chatResponse.getResult().getOutput().getText();
                        if (token != null && !token.isEmpty()) {
                            fullResponse.append(token);
                            onToken.accept(token);
                        }
                    }
                },
                error -> {
                    onError.accept("AI调用失败: " + error.getMessage());
                },
                () -> {
                    String completeContent = fullResponse.toString();
                    aiConversationService.incrementMessageCount(conversationId);
                    checkAndSummarize(conversationId, assistant);
                    onComplete.accept(completeContent);
                }
            );

        } catch (Exception e) {
            onError.accept("AI调用失败: " + e.getMessage());
        }
    }

    private List<Message> buildContext(AiAssistant assistant, Long conversationId) {
        List<Message> messages = new ArrayList<>();

        if (assistant.getSystemPrompt() != null && !assistant.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(assistant.getSystemPrompt()));
        }

        Optional<AiConversation> conversationOpt = aiConversationService.getConversationById(conversationId);
        if (conversationOpt.isPresent() && conversationOpt.get().getSummary() != null) {
            messages.add(new SystemMessage("之前的对话摘要: " + conversationOpt.get().getSummary()));
        }

        int maxContext = assistant.getMaxContext() != null ? assistant.getMaxContext() : 20;
        List<AiMessage> recentMessages = aiMessageRepository.findRecentByConversationId(
                conversationId, PageRequest.of(0, maxContext));

        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            AiMessage msg = recentMessages.get(i);
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        return messages;
    }

    private void checkAndSummarize(Long conversationId, AiAssistant assistant) {
        long messageCount = aiMessageRepository.countByConversationId(conversationId);
        if (messageCount > 0 && messageCount % SUMMARY_THRESHOLD == 0) {
            try {
                List<AiMessage> allMessages = aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
                StringBuilder history = new StringBuilder();
                for (AiMessage msg : allMessages) {
                    history.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                }

                String baseUrl = assistant.getBaseUrl();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                // 去掉用户可能带的 /chat/completions 后缀
                if (baseUrl.endsWith("/chat/completions")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - "/chat/completions".length());
                }
                boolean isGlm = baseUrl.contains("open.bigmodel.cn");

                String summary;
                if (isGlm) {
                    summary = summarizeGlm(assistant, history.toString());
                } else {
                    summary = summarizeOpenAi(assistant, baseUrl, history.toString());
                }

                if (summary != null && !summary.isEmpty()) {
                    aiConversationService.updateSummary(conversationId, summary);
                }
            } catch (Exception e) {
                // 摘要失败不影响主流程
            }
        }
    }

    private String summarizeGlm(AiAssistant assistant, String history) {
        try {
            String url = assistant.getBaseUrl();
            if (!url.endsWith("/chat/completions")) {
                url = url + "/chat/completions";
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.put("model", assistant.getModel());
            requestBody.put("temperature", 0.3);
            requestBody.put("stream", false);

            com.fasterxml.jackson.databind.node.ArrayNode messagesArray = requestBody.putArray("messages");
            ObjectNode sysMsg = messagesArray.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "请将以下对话历史压缩成一段简洁的摘要，保留关键信息：");
            ObjectNode userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", history);

            WebClient webClient = webClientBuilder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .build();

            String responseJson;
            try {
                responseJson = webClient.post()
                        .uri(url)
                        .bodyValue(mapper.writeValueAsString(requestBody))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.warn("GLM摘要 API返回错误 {} {}: body=[{}]", e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
                return null;
            }

            if (responseJson != null) {
                JsonNode node = objectMapper.readTree(responseJson);
                JsonNode choices = node.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("GLM摘要生成失败: {}", e.getMessage());
        }
        return null;
    }

    private String summarizeOpenAi(AiAssistant assistant, String baseUrl, String history) {
        try {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(assistant.getApiKey())
                    .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(assistant.getModel())
                    .temperature(0.3)
                    .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage("请将以下对话历史压缩成一段简洁的摘要，保留关键信息："),
                    new UserMessage(history)
            ));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.warn("OpenAI摘要生成失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 清理回复中可能残留的工具调用痕迹
     */
    private String cleanToolArtifacts(String content) {
        if (content == null || content.isEmpty()) return content;
        // 移除 web_fetch {...} 调用痕迹
        content = content.replaceAll("(?s)web_fetch\\s*\\{[^}]*\"url\"[^}]*\\}", "");
        // 移除 function_call 相关内容
        content = content.replaceAll("(?s)```\\s*(?:json)?\\s*\\{[^}]*(?:\"name\"|\"arguments\"|\"url\")[^}]*\\}\\s*```", "");
        // 移除孤立的 {"url": ...} JSON 片段
        content = content.replaceAll("\\{\\s*\"url\"\\s*:\\s*\"[^\"]*\"\\s*}", "");
        // 清理多余空行
        content = content.replaceAll("(?m)^\\s*$\\n{2,}", "\n\n").trim();
        return content;
    }

    /**
     * 构建当前用户消息的 multimodal content 数组。
     * 无附件时返回 null（调用方应回退为字符串 content）。
     */
    private ArrayNode buildUserMessageContent(String text, List<Map<String, String>> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        ArrayNode arr = objectMapper.createArrayNode();

        StringBuilder textPart = new StringBuilder(text == null ? "" : text);

        for (Map<String, String> att : attachments) {
            if ("image".equals(att.getOrDefault("kind", ""))) continue;
            String name = att.getOrDefault("name", "file");
            String body = att.getOrDefault("data", "");
            String lang = guessLang(name);
            if (textPart.length() > 0) textPart.append("\n\n");
            textPart.append("[文件: ").append(name).append("]\n");
            textPart.append("```").append(lang).append("\n").append(body).append("\n```");
        }

        ObjectNode textNode = objectMapper.createObjectNode();
        textNode.put("type", "text");
        textNode.put("text", textPart.toString());
        arr.add(textNode);

        for (Map<String, String> att : attachments) {
            if (!"image".equals(att.getOrDefault("kind", ""))) continue;
            String mime = att.getOrDefault("mimeType", "image/png");
            String b64 = att.getOrDefault("data", "");
            ObjectNode imgNode = objectMapper.createObjectNode();
            imgNode.put("type", "image_url");
            ObjectNode imgUrl = imgNode.putObject("image_url");
            imgUrl.put("url", "data:" + mime + ";base64," + b64);
            arr.add(imgNode);
        }

        return arr;
    }

    private String guessLang(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".c") || lower.endsWith(".h")) return "c";
        if (lower.endsWith(".cpp")) return "cpp";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".html") || lower.endsWith(".xml")) return "html";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        if (lower.endsWith(".sh")) return "bash";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".md")) return "markdown";
        return "";
    }

    private String buildToolAvailabilityPrompt(List<String> toolDefs, boolean webSearch) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 工具能力\n");
        sb.append("你拥有以下工具，可以默默使用，无需告知用户：\n");
        boolean hasWebTool = false;
        for (String toolDefJson : toolDefs) {
            try {
                JsonNode def = objectMapper.readTree(toolDefJson);
                JsonNode func = def.get("function");
                String name = func.get("name").asText();
                String desc = func.get("description").asText();
                sb.append("- ").append(name).append(": ").append(desc).append("\n");
                if ("web_fetch".equals(name) || "web_search".equals(name)) {
                    hasWebTool = true;
                }
            } catch (Exception ignored) {}
        }
        sb.append("\n## 行为规则\n");
        if (webSearch && hasWebTool) {
            sb.append("1. 【联网搜索已开启】你必须在回复前调用 web_search 工具获取最新信息。绝对不能说你无法访问网络或基于训练数据直接回答需要实时信息的问题。\n");
            sb.append("2. 对于任何涉及时事、天气、新闻、股价、赛事、价格等需要实时数据的问题，必须先用 web_search 搜索再回答。\n");
            sb.append("3. 只有当用户明确给出了一个 URL 链接时，才使用 web_fetch 抓取该网页内容。\n");
            sb.append("4. 调用工具后，基于获取到的内容自然地回答用户，就像你本来就知道这些信息一样。\n");
            sb.append("5. 绝对不要在回复中展示工具调用的过程、参数、JSON、函数名等技术细节。用户不需要知道你调用了什么工具。\n");
            sb.append("6. 不要输出类似 web_fetch、web_search、{\"url\":...}、function_call 之类的内容。\n");
            sb.append("7. 用自然、亲切、简洁的语气交流，像一个聪明的朋友在帮忙，不要用机械的口吻。\n");
        } else {
            sb.append("1. 当用户的消息中包含URL链接、或需要查询实时网络信息时，直接调用工具获取内容，不要说你无法访问网络。\n");
            sb.append("2. 调用工具后，基于获取到的内容自然地回答用户，就像你本来就知道这些信息一样。\n");
            sb.append("3. 绝对不要在回复中展示工具调用的过程、参数、JSON、函数名等技术细节。用户不需要知道你调用了什么工具。\n");
            sb.append("4. 不要输出类似 web_fetch、{\"url\":...}、function_call 之类的内容。\n");
            sb.append("5. 用自然、亲切、简洁的语气交流，像一个聪明的朋友在帮忙，不要用机械的口吻。\n");
        }
        return sb.toString();
    }

    public List<AiMessage> getConversationMessages(Long conversationId) {
        return aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * 群聊场景的流式调用：从群消息读最近 50 条作为上下文，
     * 不写 ai_messages 表（群聊归宿是 messages 表）。
     */
    public void streamGroupChat(Long assistantId, Long roomId, String triggerContent,
                                java.util.List<java.util.Map<String, String>> attachments,
                                java.util.function.Consumer<String> onToken,
                                java.util.function.Consumer<String> onComplete,
                                java.util.function.Consumer<String> onError) {
        try {
            java.util.Optional<AiAssistant> assistantOpt = aiAssistantService.getAssistantById(assistantId);
            if (assistantOpt.isEmpty()) {
                onError.accept("智能体不存在");
                return;
            }
            AiAssistant assistant = assistantOpt.get();

            java.util.List<com.chat.entity.Message> recent = messageRepository.findRecentByRoomId(
                    roomId, org.springframework.data.domain.PageRequest.of(0, 50));
            java.util.Collections.reverse(recent);

            java.util.List<Message> messages = new java.util.ArrayList<>();
            String sysPrompt = (assistant.getSystemPrompt() == null ? "" : assistant.getSystemPrompt())
                    + "\n\n你正处于群聊中，群里有多个用户和智能体。每条用户消息会以 [发送者] 前缀标明发言人。";
            messages.add(new SystemMessage(sysPrompt));

            for (com.chat.entity.Message m : recent) {
                if (m.getContent() == null || m.getContent().isEmpty()) continue;
                boolean isSelf = "assistant".equals(m.getSenderType())
                        && java.util.Objects.equals(m.getSenderId(), assistantId);
                if (isSelf) {
                    messages.add(new AssistantMessage(m.getContent()));
                } else {
                    String senderName = resolveSenderName(m);
                    messages.add(new UserMessage("[" + senderName + "] " + m.getContent()));
                }
            }

            String baseUrl = assistant.getBaseUrl();
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            String chatUrl;
            if (baseUrl.endsWith("/chat/completions") || baseUrl.contains("/chat/completions?")) {
                chatUrl = baseUrl;
            } else {
                chatUrl = baseUrl + "/chat/completions";
            }
            boolean isGlm = baseUrl.contains("open.bigmodel.cn");

            java.util.List<String> toolDefs = toolCallingService.getToolDefinitions();
            if (attachments == null) attachments = java.util.Collections.emptyList();

            if (isGlm) {
                toolCallingGlm(assistant, null, messages, toolDefs, true, attachments,
                        onToken, onComplete, onError, m -> {}, m -> {});
            } else {
                toolCallingOpenAi(assistant, null, messages, chatUrl, toolDefs, attachments,
                        onToken, onComplete, onError, m -> {}, m -> {});
            }
        } catch (Exception e) {
            onError.accept("AI 调用失败: " + e.getMessage());
        }
    }

    private String resolveSenderName(com.chat.entity.Message m) {
        if ("assistant".equals(m.getSenderType())) {
            return aiAssistantService.getAssistantById(m.getSenderId())
                    .map(AiAssistant::getName).orElse("智能体");
        }
        return userService.findById(m.getSenderId())
                .map(u -> u.getUsername()).orElse("用户");
    }
}
