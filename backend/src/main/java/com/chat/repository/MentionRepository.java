package com.chat.repository;

import com.chat.entity.Mention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentionRepository extends JpaRepository<Mention, String> {
    
    @Query("SELECT m FROM Mention m WHERE m.roomId = :roomId " +
           "AND (m.mentionedUserId = :userId OR m.isAll = true) " +
           "AND NOT EXISTS (SELECT r FROM MentionReadReceipt r WHERE r.mentionId = m.id AND r.userId = :userId)")
    List<Mention> findUnreadMentions(@Param("userId") Long userId, @Param("roomId") Long roomId);
    
    @Query("SELECT COUNT(m) FROM Mention m WHERE m.roomId = :roomId " +
           "AND (m.mentionedUserId = :userId OR m.isAll = true) " +
           "AND NOT EXISTS (SELECT r FROM MentionReadReceipt r WHERE r.mentionId = m.id AND r.userId = :userId)")
    long countUnreadMentions(@Param("userId") Long userId, @Param("roomId") Long roomId);
    
    List<Mention> findByMessageId(String messageId);
}
