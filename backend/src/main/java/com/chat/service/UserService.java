package com.chat.service;

import com.chat.entity.User;
import com.chat.exception.BusinessException;
import com.chat.exception.UserBannedException;
import com.chat.repository.UserRepository;
import com.chat.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Transactional
    @CacheEvict(value = {"users", "usersByName"}, allEntries = true)
    public User register(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setId(idGenerator.nextId());
        user.setUsername(username);
        user.setCreatedAt(System.currentTimeMillis());
        user.setLastSeen(System.currentTimeMillis());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在，请先注册"));
        if (user.isBanned()) {
            throw new UserBannedException(user.getBannedReason());
        }
        return user;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usersByName", key = "#username")
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminUserList() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> userMap = new java.util.HashMap<>();
                    userMap.put("userId", String.valueOf(user.getId()));
                    userMap.put("username", user.getUsername());
                    userMap.put("createdAt", user.getCreatedAt());
                    userMap.put("lastSeen", user.getLastSeen() == null ? 0L : user.getLastSeen());
                    userMap.put("banned", user.isBanned());
                    userMap.put("bannedAt", user.getBannedAt() == null ? 0L : user.getBannedAt());
                    userMap.put("bannedReason", user.getBannedReason() == null ? "" : user.getBannedReason());
                    return userMap;
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"users", "usersByName"}, allEntries = true)
    public void renameUser(Long userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        String trimmedUsername = username.trim();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Optional<User> existingUser = userRepository.findByUsername(trimmedUsername);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new BusinessException("用户名已存在");
        }
        user.setUsername(trimmedUsername);
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "usersByName"}, allEntries = true)
    public void banUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setBanned(true);
        user.setBannedAt(System.currentTimeMillis());
        user.setBannedReason(reason == null ? null : reason.trim());
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "usersByName"}, allEntries = true)
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setBanned(false);
        user.setBannedAt(null);
        user.setBannedReason(null);
        userRepository.save(user);
    }

    @Transactional
    public void updateLastSeen(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeen(System.currentTimeMillis());
            userRepository.save(user);
        });
    }
}
