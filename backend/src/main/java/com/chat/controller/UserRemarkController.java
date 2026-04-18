package com.chat.controller;

import com.chat.entity.UserRemark;
import com.chat.service.UserRemarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user-remarks")
@CrossOrigin(origins = "*")
public class UserRemarkController {

    @Autowired
    private UserRemarkService userRemarkService;

    @GetMapping
    public ResponseEntity<?> getRemarks(@RequestParam Long userId) {
        Map<Long, String> remarkMap = userRemarkService.getRemarkMap(userId);
        Map<String, String> response = new HashMap<>();
        remarkMap.forEach((targetUserId, remarkName) -> response.put(String.valueOf(targetUserId), remarkName));
        return ResponseEntity.ok(Map.of("remarks", response));
    }

    @PostMapping
    public ResponseEntity<?> saveRemark(@RequestBody Map<String, Object> request) {
        Long userId = parseLongId(request.get("userId"));
        Long targetUserId = parseLongId(request.get("targetUserId"));
        String remarkName = (String) request.get("remarkName");

        if (userId == null || targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数错误"));
        }

        try {
            UserRemark remark = userRemarkService.saveRemark(userId, targetUserId, remarkName);
            return ResponseEntity.ok(Map.of(
                    "id", String.valueOf(remark.getId()),
                    "userId", String.valueOf(remark.getUserId()),
                    "targetUserId", String.valueOf(remark.getTargetUserId()),
                    "remarkName", remark.getRemarkName(),
                    "updatedAt", remark.getUpdatedAt()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long parseLongId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Invalid ID value: " + value + " (type: " + value.getClass().getName() + ")");
    }
}
