package com.chat.service;

import com.chat.entity.Message;
import com.chat.entity.RoomMember;
import com.chat.entity.User;
import com.chat.repository.MessageRepository;
import com.chat.repository.RoomMemberRepository;
import com.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRemarkService userRemarkService;

    @Transactional
    public Message sendMessage(Long roomId, Long senderId, String content, String type) {
        Long seq = getNextSeq(roomId);

        Message msg = new Message();
        msg.setId(UUID.randomUUID().toString());
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setType(type != null ? type : "text");
        msg.setSeq(seq);
        msg.setTimestamp(System.currentTimeMillis());

        return messageRepository.save(msg);
    }

    @Transactional
    public Message sendFileMessage(Long roomId, Long senderId, String content,
                                   String fileId, String fileName, String fileUrl,
                                   Long fileSize, String fileType) {
        Long seq = getNextSeq(roomId);

        Message msg = new Message();
        msg.setId(UUID.randomUUID().toString());
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setType("file");
        msg.setSeq(seq);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setFileId(fileId);
        msg.setFileName(fileName);
        msg.setFileUrl(fileUrl);
        msg.setFileSize(fileSize);
        msg.setFileType(fileType);

        return messageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<Message> getHistory(Long roomId, Long lastSeq) {
        return messageRepository.findMessagesAfterSeq(roomId,
            lastSeq != null ? lastSeq : 0);
    }

    @Transactional(readOnly = true)
    public List<Message> getLatestMessages(Long roomId, int limit) {
        return messageRepository.findLatestMessages(roomId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Optional<Message> getLatestMessage(Long roomId) {
        return messageRepository.findLatestMessage(roomId);
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessages(Long roomId) {
        return messageRepository.findByRoomIdOrderBySeqAsc(roomId);
    }

    @Transactional
    public void updateLastReadSeq(Long roomId, Long userId, Long seq) {
        Optional<RoomMember> memberOpt = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (memberOpt.isPresent()) {
            RoomMember member = memberOpt.get();
            member.setLastReadSeq(seq);
            roomMemberRepository.save(member);
        }
    }

    @Transactional(readOnly = true)
    public Long getLastReadSeq(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
            .map(RoomMember::getLastReadSeq)
            .orElse(0L);
    }

    @Transactional(readOnly = true)
    public String getSenderName(Long senderId) {
        return userRepository.findById(senderId)
            .map(User::getUsername)
            .orElse("未知用户");
    }

    @Transactional(readOnly = true)
    public String getSenderName(Long viewerId, Long senderId) {
        String defaultName = getSenderName(senderId);
        if (viewerId == null) {
            return defaultName;
        }
        return userRemarkService.getDisplayName(viewerId, senderId, defaultName);
    }

    private synchronized Long getNextSeq(Long roomId) {
        Long maxSeq = messageRepository.findMaxSeqByRoomId(roomId).orElse(0L);
        return maxSeq + 1;
    }
}
