package com.chat.repository;

import com.chat.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findByUserIdAndAssistantIdOrderByUpdatedAtDesc(Long userId, Long assistantId);
    
    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    long countByUserId(Long userId);
}
