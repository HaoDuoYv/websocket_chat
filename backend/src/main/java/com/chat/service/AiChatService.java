package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.AiConversation;
import com.chat.entity.AiMessage;
import com.chat.repository.AiMessageRepository;
import com.chat.utils.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            // 处理baseUrl - Spring AI会自动在baseUrl后添加 /v1/chat/completions
            String baseUrl = assistant.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            // 如果baseUrl以/v1结尾，移除它（Spring AI会自动添加）
            if (baseUrl.endsWith("/v1")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
            }

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
                    // 消息保存移到ChatWebSocketHandler中，避免重复保存
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

                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl(assistant.getBaseUrl())
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
                        new UserMessage(history.toString())
                ));

                org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
                String summary = response.getResult().getOutput().getText();
                
                aiConversationService.updateSummary(conversationId, summary);
            } catch (Exception e) {
                // 摘要失败不影响主流程
            }
        }
    }

    public List<AiMessage> getConversationMessages(Long conversationId) {
        return aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
