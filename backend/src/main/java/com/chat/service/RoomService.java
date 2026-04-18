package com.chat.service;

import com.chat.entity.Room;
import com.chat.entity.RoomMember;
import com.chat.entity.User;
import com.chat.repository.RoomMemberRepository;
import com.chat.repository.RoomRepository;
import com.chat.repository.UserRepository;
import com.chat.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRemarkService userRemarkService;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Transactional
    public Room createPublicRoom(String name, Long ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Room room = new Room();
        room.setId(idGenerator.nextId());
        room.setName(name);
        room.setType("public");
        room.setOwnerId(ownerId);
        room.setCreatedAt(System.currentTimeMillis());

        Room savedRoom = roomRepository.save(room);

        RoomMember member = new RoomMember();
        member.setRoomId(savedRoom.getId());
        member.setUserId(ownerId);
        member.setJoinedAt(System.currentTimeMillis());
        roomMemberRepository.save(member);

        return savedRoom;
    }

    @Transactional
    public Room getOrCreatePrivateRoom(Long userId1, Long userId2) {
        Optional<Room> existingRoom = roomRepository.findPrivateRoomBetweenUsers("private", userId1, userId2);
        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        userRepository.findById(userId1)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Room room = new Room();
        room.setId(idGenerator.nextId());
        room.setName(user2.getUsername());
        room.setType("private");
        room.setCreatedAt(System.currentTimeMillis());

        Room savedRoom = roomRepository.save(room);

        RoomMember member1 = new RoomMember();
        member1.setRoomId(savedRoom.getId());
        member1.setUserId(userId1);
        member1.setJoinedAt(System.currentTimeMillis());
        roomMemberRepository.save(member1);

        RoomMember member2 = new RoomMember();
        member2.setRoomId(savedRoom.getId());
        member2.setUserId(userId2);
        member2.setJoinedAt(System.currentTimeMillis());
        roomMemberRepository.save(member2);

        return savedRoom;
    }

    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (existingMember.isPresent()) {
            return;
        }

        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setJoinedAt(System.currentTimeMillis());
        roomMemberRepository.save(member);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        roomMemberRepository.deleteById(new com.chat.entity.RoomMemberId(roomId, userId));
    }

    @Transactional(readOnly = true)
    public List<Room> getUserRooms(Long userId) {
        return roomRepository.findByMembers_Id(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Room> findById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    @Transactional(readOnly = true)
    public boolean isRoomMember(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Long> getRoomMemberIds(Long roomId) {
        return roomMemberRepository.findByRoomId(roomId).stream()
                .map(RoomMember::getUserId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getPrivateRoomDisplayName(Long viewerId, Long roomId, String defaultName) {
        List<Long> memberIds = getRoomMemberIds(roomId);
        Long partnerId = memberIds.stream()
                .filter(id -> !id.equals(viewerId))
                .findFirst()
                .orElse(null);
        if (partnerId == null) {
            return defaultName;
        }
        return userRepository.findById(partnerId)
                .map(User::getUsername)
                .map(username -> userRemarkService.getDisplayName(viewerId, partnerId, username))
                .orElse(defaultName);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        roomMemberRepository.deleteByRoomId(roomId);
        roomRepository.deleteById(roomId);
    }
}
