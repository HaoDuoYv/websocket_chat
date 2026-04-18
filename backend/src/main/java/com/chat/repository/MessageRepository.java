package com.chat.repository;

import com.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId " +
           "AND m.seq > :lastSeq ORDER BY m.seq ASC")
    List<Message> findMessagesAfterSeq(@Param("roomId") Long roomId,
                                       @Param("lastSeq") Long lastSeq);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId " +
           "ORDER BY m.seq DESC")
    List<Message> findLatestMessages(@Param("roomId") Long roomId,
                                     org.springframework.data.domain.Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId ORDER BY m.seq DESC LIMIT 1")
    Optional<Message> findLatestMessage(@Param("roomId") Long roomId);

    @Query("SELECT MAX(m.seq) FROM Message m WHERE m.roomId = :roomId")
    Optional<Long> findMaxSeqByRoomId(@Param("roomId") Long roomId);

    List<Message> findByRoomIdOrderBySeqAsc(Long roomId);
}
