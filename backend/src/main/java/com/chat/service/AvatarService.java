package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.User;
import com.chat.properties.LocalProperties;
import com.chat.repository.AiAssistantRepository;
import com.chat.repository.UserRepository;
import com.chat.utils.LocalUploadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class AvatarService {

    private static final String AVATAR_SUB_DIR = "avatars";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    ));

    @Autowired
    private LocalProperties localProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiAssistantRepository aiAssistantRepository;

    @Value("${server.port:8081}")
    private int configuredServerPort;

    /**
     * 上传用户头像
     */
    public String uploadUserAvatar(Long userId, MultipartFile file, String scheme, String serverName, int serverPort) throws IOException {
        validateFile(file);

        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        String relativeUrl = uploadUtil.uploadWithInfo(file, AVATAR_SUB_DIR, null, String.valueOf(userId));
        int resolvedPort = resolvePort(serverPort);
        String absoluteUrl = uploadUtil.toAbsoluteFileUrl(relativeUrl, scheme, serverName, resolvedPort);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAvatarUrl(absoluteUrl);
            userRepository.save(user);
            log.info("用户头像更新成功 - userId: {}, url: {}", userId, absoluteUrl);
        } else {
            throw new RuntimeException("用户不存在: " + userId);
        }

        return absoluteUrl;
    }

    /**
     * 上传AI助手头像
     */
    public String uploadAiAvatar(Long assistantId, MultipartFile file, String scheme, String serverName, int serverPort) throws IOException {
        validateFile(file);

        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        String relativeUrl = uploadUtil.uploadWithInfo(file, AVATAR_SUB_DIR, null, "ai_" + assistantId);
        int resolvedPort = resolvePort(serverPort);
        String absoluteUrl = uploadUtil.toAbsoluteFileUrl(relativeUrl, scheme, serverName, resolvedPort);

        Optional<AiAssistant> assistantOpt = aiAssistantRepository.findById(assistantId);
        if (assistantOpt.isPresent()) {
            AiAssistant assistant = assistantOpt.get();
            assistant.setAvatarUrl(absoluteUrl);
            aiAssistantRepository.save(assistant);
            log.info("AI助手头像更新成功 - assistantId: {}, url: {}", assistantId, absoluteUrl);
        } else {
            throw new RuntimeException("AI助手不存在: " + assistantId);
        }

        return absoluteUrl;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("头像大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("只支持 JPG、PNG、GIF、WebP 格式的图片");
        }
    }

    private int resolvePort(int requestPort) {
        if (requestPort > 0 && requestPort != 80 && requestPort != 443) {
            return requestPort;
        }
        return configuredServerPort;
    }
}
