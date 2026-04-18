package com.chat.repository;

import com.chat.entity.UserRemark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRemarkRepository extends JpaRepository<UserRemark, Long> {
    Optional<UserRemark> findByUserIdAndTargetUserId(Long userId, Long targetUserId);
    List<UserRemark> findByUserId(Long userId);
}
