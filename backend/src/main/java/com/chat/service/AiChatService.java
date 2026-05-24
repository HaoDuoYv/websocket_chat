package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.AiConversation;
import com.chat.entity.AiMessage;
import com.chat.repository.AiMessageRepository;
import com.chat.utils.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class AiChatService {

    @Autowired
    private AiMessageRepository aiMessageRepository;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    private static final int SUMMARY_THRESHOLD = 20;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AiMessage saveUserMessage(Long conversationId, String content) {
        AiMessage message = new AiMessage();
        message.setId(idGenerator.nextId());
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        message.setCreatedAt(System.currentTimeMillis());
        return aiMessageRepository.save(message);
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

    public void streamChat(Long assistantId, Long conversationId, String userContent, Consumer<String> onToken, Consumer<String> onComplete, Consumer<String> onError) {
        try {
            Optional<AiAssistant> assistantOpt = aiAssistantService.getAssistantById(assistantId);
            if (assistantOpt.isEmpty()) {
                onError.accept("AI助手不存在");
                return;
            }
            AiAssistant assistant = assistantOpt.get();

            saveUserMessage(conversationId, userContent);
            aiConversationService.incrementMessageCount(conversationId);

            List<Message> messages = buildContext(assistant, conversationId);

            String baseUrl = assistant.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (baseUrl.endsWith("/v1")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
            }

            boolean isGlm = baseUrl.contains("open.bigmodel.cn");

            if (isGlm) {
                streamGlmChat(assistant, conversationId, userContent, onToken, onComplete, onError);
            } else {
                streamOpenAiChat(assistant, conversationId, messages, baseUrl, onToken, onComplete, onError);
            }

        } catch (Exception e) {
            onError.accept("AI调用失败: " + e.getMessage());
        }
    }

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
            
            // 系统提示
            if (assistant.getSystemPrompt() != null && !assistant.getSystemPrompt().isEmpty()) {
                ObjectNode sysMsg = messagesArray.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", assistant.getSystemPrompt());
            }
            
            // 对话摘要
            Optional<AiConversation> conversationOpt = aiConversationService.getConversationById(conversationId);
            if (conversationOpt.isPresent() && conversationOpt.get().getSummary() != null) {
                ObjectNode summaryMsg = messagesArray.addObject();
                summaryMsg.put("role", "system");
                summaryMsg.put("content", "之前的对话摘要: " + conversationOpt.get().getSummary());
            }
            
            // 历史消息
            int maxContext = assistant.getMaxContext() != null ? assistant.getMaxContext() : 20;
            List<AiMessage> recentMessages = aiMessageRepository.findRecentByConversationId(conversationId, PageRequest.of(0, maxContext));
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                AiMessage msg = recentMessages.get(i);
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
            }
            
            // 当前用户消息
            ObjectNode userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            String bodyJson = mapper.writeValueAsString(requestBody);
            log.info("GLM请求: {}", bodyJson);

            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
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
                            String errorMsg = error.getMessage();
                            if (errorMsg != null && errorMsg.contains("400")) {
                                onError.accept("GLM API调用失败(400)：请检查模型名称和temperature(0-1)。原始错误: " + errorMsg);
                            } else if (errorMsg != null && errorMsg.contains("401")) {
                                onError.accept("GLM API认证失败(401)：请检查API Key。");
                            } else {
                                onError.accept("GLM API调用失败: " + errorMsg);
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
                if (baseUrl.endsWith("/v1")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
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

            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + assistant.getApiKey())
                    .build();

            String responseJson = webClient.post()
                    .uri(url)
                    .bodyValue(mapper.writeValueAsString(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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

    public List<AiMessage> getConversationMessages(Long conversationId) {
        return aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
