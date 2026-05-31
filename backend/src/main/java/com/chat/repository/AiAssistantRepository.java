package com.chat.repository;

import com.chat.entity.AiAssistant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiAssistantRepository extends JpaRepository<AiAssistant, Long> {
    List<AiAssistant> findByIsSystemTrue();

    List<AiAssistant> findByIsPublicTrueOrIsSystemTrue();

    List<AiAssistant> findByOwnerId(Long ownerId);
    
    Optional<AiAssistant> findByShareCode(String shareCode);
    
    Optional<AiAssistant> findByOwnerIdAndId(Long ownerId, Long id);
}
