package com.chat.repository;

import com.chat.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    @Query("SELECT m FROM AiMessage m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<AiMessage> findRecentByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
    
    long countByConversationId(Long conversationId);
    
    @Modifying
    @Transactional
    void deleteByConversationId(Long conversationId);
}
