package com.chat.service;

import com.chat.entity.AiConversation;
import com.chat.exception.BusinessException;
import com.chat.repository.AiConversationRepository;
import com.chat.repository.AiMessageRepository;
import com.chat.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AiConversationService {

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiMessageRepository aiMessageRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Transactional
    public AiConversation createConversation(Long userId, Long assistantId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setId(idGenerator.nextId());
        conversation.setUserId(userId);
        conversation.setAssistantId(assistantId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setMessageCount(0);
        conversation.setCreatedAt(System.currentTimeMillis());
        conversation.setUpdatedAt(System.currentTimeMillis());
        return aiConversationRepository.save(conversation);
    }

    public List<AiConversation> getUserConversations(Long userId, Long assistantId) {
        return aiConversationRepository.findByUserIdAndAssistantIdOrderByUpdatedAtDesc(userId, assistantId);
    }

    public List<AiConversation> getUserAllConversations(Long userId) {
        return aiConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<AiConversation> getConversationById(Long id) {
        return aiConversationRepository.findById(id);
    }

    @Transactional
    public AiConversation updateTitle(Long conversationId, String title) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        conversation.setTitle(title);
        conversation.setUpdatedAt(System.currentTimeMillis());
        return aiConversationRepository.save(conversation);
    }

    @Transactional
    public AiConversation updateSummary(Long conversationId, String summary) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        conversation.setSummary(summary);
        conversation.setUpdatedAt(System.currentTimeMillis());
        return aiConversationRepository.save(conversation);
    }

    @Transactional
    public void incrementMessageCount(Long conversationId) {
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversation.setUpdatedAt(System.currentTimeMillis());
        aiConversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        // 先删除会话的所有消息
        aiMessageRepository.deleteByConversationId(conversationId);
        // 再删除会话
        aiConversationRepository.deleteById(conversationId);
    }
}
