package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.exception.BusinessException;
import com.chat.repository.AiAssistantRepository;
import com.chat.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiAssistantService {

    @Autowired
    private AiAssistantRepository aiAssistantRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    public AiAssistant getSystemAssistant() {
        List<AiAssistant> systemAssistants = aiAssistantRepository.findByIsSystemTrue();
        return systemAssistants.isEmpty() ? null : systemAssistants.get(0);
    }

    @Transactional
    public AiAssistant saveSystemAssistant(AiAssistant assistant) {
        AiAssistant existing = getSystemAssistant();
        if (existing != null) {
            existing.setName(assistant.getName());
            existing.setAvatarIcon(assistant.getAvatarIcon());
            existing.setAvatarColor(assistant.getAvatarColor());
            existing.setSystemPrompt(assistant.getSystemPrompt());
            existing.setBaseUrl(assistant.getBaseUrl());
            existing.setApiKey(assistant.getApiKey());
            existing.setModel(assistant.getModel());
            existing.setTemperature(assistant.getTemperature());
            existing.setMaxContext(assistant.getMaxContext());
            existing.setMaxTokens(assistant.getMaxTokens());
            existing.setUpdatedAt(System.currentTimeMillis());
            return aiAssistantRepository.save(existing);
        } else {
            assistant.setId(idGenerator.nextId());
            assistant.setIsSystem(true);
            assistant.setIsPublic(false);
            assistant.setCreatedAt(System.currentTimeMillis());
            assistant.setUpdatedAt(System.currentTimeMillis());
            return aiAssistantRepository.save(assistant);
        }
    }

    @Transactional
    public AiAssistant createUserAssistant(Long ownerId, AiAssistant assistant) {
        assistant.setId(idGenerator.nextId());
        assistant.setOwnerId(ownerId);
        assistant.setIsSystem(false);
        assistant.setShareCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        assistant.setCreatedAt(System.currentTimeMillis());
        assistant.setUpdatedAt(System.currentTimeMillis());
        return aiAssistantRepository.save(assistant);
    }

    @Transactional
    public AiAssistant updateUserAssistant(Long ownerId, Long assistantId, AiAssistant assistant) {
        AiAssistant existing = aiAssistantRepository.findByOwnerIdAndId(ownerId, assistantId)
                .orElseThrow(() -> new BusinessException("AI助手不存在"));
        
        existing.setName(assistant.getName());
        existing.setAvatarIcon(assistant.getAvatarIcon());
        existing.setAvatarColor(assistant.getAvatarColor());
        existing.setSystemPrompt(assistant.getSystemPrompt());
        existing.setBaseUrl(assistant.getBaseUrl());
        existing.setApiKey(assistant.getApiKey());
        existing.setModel(assistant.getModel());
        existing.setTemperature(assistant.getTemperature());
        existing.setMaxContext(assistant.getMaxContext());
        existing.setMaxTokens(assistant.getMaxTokens());
        existing.setUpdatedAt(System.currentTimeMillis());
        return aiAssistantRepository.save(existing);
    }

    @Transactional
    public void deleteUserAssistant(Long ownerId, Long assistantId) {
        AiAssistant assistant = aiAssistantRepository.findByOwnerIdAndId(ownerId, assistantId)
                .orElseThrow(() -> new BusinessException("AI助手不存在"));
        aiAssistantRepository.delete(assistant);
    }

    public List<AiAssistant> getUserAssistants(Long ownerId) {
        return aiAssistantRepository.findByOwnerId(ownerId);
    }

    public List<AiAssistant> getAllPublicOrSystem() {
        return aiAssistantRepository.findByIsPublicTrueOrIsSystemTrue();
    }

    public Optional<AiAssistant> getAssistantById(Long id) {
        return aiAssistantRepository.findById(id);
    }

    public Optional<AiAssistant> getAssistantByShareCode(String shareCode) {
        return aiAssistantRepository.findByShareCode(shareCode);
    }

    @Transactional
    public AiAssistant copyAssistantByShareCode(Long userId, String shareCode) {
        AiAssistant source = aiAssistantRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new BusinessException("分享码无效"));
        
        AiAssistant copy = new AiAssistant();
        copy.setId(idGenerator.nextId());
        copy.setOwnerId(userId);
        copy.setName(source.getName());
        copy.setAvatarIcon(source.getAvatarIcon());
        copy.setAvatarColor(source.getAvatarColor());
        copy.setSystemPrompt(source.getSystemPrompt());
        copy.setBaseUrl(source.getBaseUrl());
        copy.setApiKey(source.getApiKey());
        copy.setModel(source.getModel());
        copy.setTemperature(source.getTemperature());
        copy.setMaxContext(source.getMaxContext());
        copy.setMaxTokens(source.getMaxTokens());
        copy.setIsSystem(false);
        copy.setIsPublic(false);
        copy.setShareCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        copy.setCreatedAt(System.currentTimeMillis());
        copy.setUpdatedAt(System.currentTimeMillis());
        
        return aiAssistantRepository.save(copy);
    }
}
