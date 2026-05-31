package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.Mention;
import com.chat.entity.MentionReadReceipt;
import com.chat.entity.Message;
import com.chat.repository.MentionRepository;
import com.chat.repository.MentionReadReceiptRepository;
import com.chat.repository.MessageRepository;
import com.chat.repository.RoomMemberRepository;
import com.chat.utils.SnowflakeIdGenerator;
import com.chat.vo.MentionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MentionService {

    @Autowired
    private MentionRepository mentionRepository;

    @Autowired
    private MentionReadReceiptRepository mentionReadReceiptRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private final ConcurrentHashMap<String, Long> unreadCountCache = new ConcurrentHashMap<>();

    public List<Mention> createMentions(String messageId, Long roomId, Long senderId, 
                                         List<Long> mentionedUserIds, boolean mentionAll) {
        List<Mention> mentions = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (mentionAll) {
            Mention mention = new Mention();
            mention.setId(UUID.randomUUID().toString());
            mention.setMessageId(messageId);
            mention.setRoomId(roomId);
            mention.setSenderId(senderId);
            mention.setMentionedUserId(null);
            mention.setAll(true);
            mention.setCreatedAt(now);
            mentions.add(mentionRepository.save(mention));
        } else if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
            for (Long userId : mentionedUserIds) {
                Mention mention = new Mention();
                mention.setId(UUID.randomUUID().toString());
                mention.setMessageId(messageId);
                mention.setRoomId(roomId);
                mention.setSenderId(senderId);
                mention.setMentionedUserId(userId);
                mention.setAll(false);
                mention.setCreatedAt(now);
                mentions.add(mentionRepository.save(mention));
            }
        }

        return mentions;
    }

    public long getUnreadMentionCount(Long userId, Long roomId) {
        String key = roomId + ":" + userId;
        return unreadCountCache.computeIfAbsent(key, k -> 
            mentionRepository.countUnreadMentions(userId, roomId));
    }

    public List<MentionVO> getUnreadMentions(Long userId, Long roomId) {
        List<Mention> unreadMentions = mentionRepository.findUnreadMentions(userId, roomId);
        List<MentionVO> result = new ArrayList<>();

        for (Mention mention : unreadMentions) {
            Optional<Message> messageOpt = messageRepository.findById(mention.getMessageId());
            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                MentionVO vo = new MentionVO();
                vo.setId(mention.getId());
                vo.setMessageId(mention.getMessageId());
                vo.setRoomId(mention.getRoomId());
                vo.setSenderId(mention.getSenderId());
                vo.setContent(message.getContent());
                vo.setAll(mention.isAll());
                vo.setTimestamp(mention.getCreatedAt());
                result.add(vo);
            }
        }

        return result;
    }

    public void markMentionsAsRead(Long userId, Long roomId) {
        List<Mention> unreadMentions = mentionRepository.findUnreadMentions(userId, roomId);
        
        for (Mention mention : unreadMentions) {
            if (!mentionReadReceiptRepository.existsByMentionIdAndUserId(mention.getId(), userId)) {
                MentionReadReceipt receipt = new MentionReadReceipt();
                receipt.setId(snowflakeIdGenerator.nextId());
                receipt.setMentionId(mention.getId());
                receipt.setUserId(userId);
                receipt.setReadAt(System.currentTimeMillis());
                mentionReadReceiptRepository.save(receipt);
            }
        }

        clearUnreadCount(userId, roomId);
    }

    public void incrementUnreadCount(Long userId, Long roomId) {
        String key = roomId + ":" + userId;
        unreadCountCache.compute(key, (k, v) -> v == null ? 1 : v + 1);
    }

    public void clearUnreadCount(Long userId, Long roomId) {
        String key = roomId + ":" + userId;
        unreadCountCache.remove(key);
    }

    public List<Long> getRoomMemberIds(Long roomId) {
        return roomMemberRepository.findByRoomId(roomId)
                .stream()
                .map(member -> member.getUserId())
                .toList();
    }

    public List<AiAssistant> extractAssistantMentions(Long roomId, List<Long> mentionedIds) {
        if (mentionedIds == null || mentionedIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<AiAssistant> result = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (Long id : mentionedIds) {
            if (id == null || !seen.add(id)) continue;
            boolean isAssistantMember = roomMemberRepository
                    .findByRoomIdAndUserIdAndMemberType(roomId, id, "assistant")
                    .isPresent();
            if (!isAssistantMember) continue;
            aiAssistantService.getAssistantById(id).ifPresent(result::add);
        }
        return result;
    }
}
