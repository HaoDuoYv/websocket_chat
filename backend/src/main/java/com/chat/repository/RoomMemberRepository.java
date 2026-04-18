package com.chat.repository;

import com.chat.entity.RoomMember;
import com.chat.entity.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    List<RoomMember> findByRoomId(Long roomId);

    List<RoomMember> findByUserId(Long userId);

    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    void deleteByRoomId(Long roomId);
}
