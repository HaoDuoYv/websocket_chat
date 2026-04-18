package com.chat.service;

import com.chat.entity.UserRemark;
import com.chat.repository.UserRemarkRepository;
import com.chat.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserRemarkService {

    @Autowired
    private UserRemarkRepository userRemarkRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Transactional
    public UserRemark saveRemark(Long userId, Long targetUserId, String remarkName) {
        String normalizedRemark = remarkName == null ? "" : remarkName.trim();
        if (normalizedRemark.isEmpty()) {
            throw new RuntimeException("备注名不能为空");
        }
        if (normalizedRemark.length() > 100) {
            throw new RuntimeException("备注名不能超过100个字符");
        }
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能给自己设置备注");
        }
        userService.findById(targetUserId).orElseThrow(() -> new RuntimeException("目标用户不存在"));

        long now = System.currentTimeMillis();
        Optional<UserRemark> existingRemark = userRemarkRepository.findByUserIdAndTargetUserId(userId, targetUserId);
        UserRemark userRemark = existingRemark.orElseGet(UserRemark::new);
        if (userRemark.getId() == null) {
            userRemark.setId(idGenerator.nextId());
            userRemark.setUserId(userId);
            userRemark.setTargetUserId(targetUserId);
            userRemark.setCreatedAt(now);
        }
        userRemark.setRemarkName(normalizedRemark);
        userRemark.setUpdatedAt(now);
        return userRemarkRepository.save(userRemark);
    }

    @Transactional(readOnly = true)
    public Optional<UserRemark> findRemark(Long userId, Long targetUserId) {
        return userRemarkRepository.findByUserIdAndTargetUserId(userId, targetUserId);
    }

    @Transactional(readOnly = true)
    public String getDisplayName(Long userId, Long targetUserId, String defaultName) {
        return findRemark(userId, targetUserId)
                .map(UserRemark::getRemarkName)
                .filter(name -> !name.isBlank())
                .orElse(defaultName);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getRemarkMap(Long userId) {
        List<UserRemark> remarks = userRemarkRepository.findByUserId(userId);
        Map<Long, String> remarkMap = new HashMap<>();
        for (UserRemark remark : remarks) {
            remarkMap.put(remark.getTargetUserId(), remark.getRemarkName());
        }
        return remarkMap;
    }
}
