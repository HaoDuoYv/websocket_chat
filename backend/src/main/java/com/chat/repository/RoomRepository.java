package com.chat.repository;

import com.chat.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r JOIN RoomMember rm ON r.id = rm.roomId WHERE rm.userId = :userId")
    List<Room> findByMembers_Id(@Param("userId") Long userId);

    @Query("SELECT r FROM Room r WHERE r.type = :type AND " +
           "EXISTS (SELECT 1 FROM RoomMember rm1 WHERE rm1.roomId = r.id AND rm1.userId = :userId1) AND " +
           "EXISTS (SELECT 1 FROM RoomMember rm2 WHERE rm2.roomId = r.id AND rm2.userId = :userId2)")
    Optional<Room> findPrivateRoomBetweenUsers(@Param("type") String type,
                                                @Param("userId1") Long userId1,
                                                @Param("userId2") Long userId2);
}
