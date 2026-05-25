package com.chat.repository;

import com.chat.entity.MentionReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MentionReadReceiptRepository extends JpaRepository<MentionReadReceipt, Long> {
    
    Optional<MentionReadReceipt> findByMentionIdAndUserId(String mentionId, Long userId);
    
    boolean existsByMentionIdAndUserId(String mentionId, Long userId);
}
