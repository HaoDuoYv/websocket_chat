package com.chat.service;

import com.chat.entity.AiAssistant;
import com.chat.entity.Room;
import com.chat.entity.User;
import com.chat.properties.LocalProperties;
import com.chat.repository.AiAssistantRepository;
import com.chat.repository.RoomRepository;
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
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    ));

    @Autowired
    private LocalProperties localProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiAssistantRepository aiAssistantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Value("${server.port:8081}")
    private int configuredServerPort;

    /**
     * 上传用户头像到临时目录（第一阶段）
     * @return temp 相对路径
     */
    public String uploadUserAvatarTemp(Long userId, MultipartFile file) throws IOException {
        validateFile(file);
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        return uploadUtil.uploadToTemp(file);
    }

    /**
     * 确认用户头像（第二阶段）：从 temp 移到正式目录，删除旧头像
     */
    public String confirmUserAvatar(Long userId, String tempPath,
                                     String scheme, String serverName, int serverPort) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        User user = userOpt.get();

        // 删除旧头像文件
        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            uploadUtil.deleteFile(oldAvatarUrl);
        }

        // 从 temp 移到正式目录
        int resolvedPort = resolvePort(serverPort);
        String relativeUrl;
        try {
            relativeUrl = uploadUtil.moveFromTemp(tempPath, AVATAR_SUB_DIR);
        } catch (IOException e) {
            throw new RuntimeException("移动头像文件失败", e);
        }
        String absoluteUrl = uploadUtil.toAbsoluteFileUrl(relativeUrl, scheme, serverName, resolvedPort);

        user.setAvatarUrl(absoluteUrl);
        userRepository.save(user);
        log.info("用户头像确认成功 - userId: {}, url: {}", userId, absoluteUrl);

        return absoluteUrl;
    }

    /**
     * 取消头像上传：删除临时文件
     */
    public void cancelUserAvatar(String tempPath) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        uploadUtil.deleteTempFile(tempPath);
        log.info("头像上传已取消，临时文件已删除: {}", tempPath);
    }

    /**
     * 上传AI助手头像到临时目录（第一阶段）
     */
    public String uploadAiAvatarTemp(Long assistantId, MultipartFile file) throws IOException {
        validateFile(file);
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        return uploadUtil.uploadToTemp(file);
    }

    /**
     * 确认AI助手头像（第二阶段）
     */
    public String confirmAiAvatar(Long assistantId, String tempPath,
                                   String scheme, String serverName, int serverPort) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);

        Optional<AiAssistant> assistantOpt = aiAssistantRepository.findById(assistantId);
        if (assistantOpt.isEmpty()) {
            throw new RuntimeException("AI助手不存在: " + assistantId);
        }
        AiAssistant assistant = assistantOpt.get();

        String oldAvatarUrl = assistant.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            uploadUtil.deleteFile(oldAvatarUrl);
        }

        int resolvedPort = resolvePort(serverPort);
        String relativeUrl;
        try {
            relativeUrl = uploadUtil.moveFromTemp(tempPath, AVATAR_SUB_DIR);
        } catch (IOException e) {
            throw new RuntimeException("移动头像文件失败", e);
        }
        String absoluteUrl = uploadUtil.toAbsoluteFileUrl(relativeUrl, scheme, serverName, resolvedPort);

        assistant.setAvatarUrl(absoluteUrl);
        aiAssistantRepository.save(assistant);
        log.info("AI助手头像确认成功 - assistantId: {}, url: {}", assistantId, absoluteUrl);

        return absoluteUrl;
    }

    /**
     * 取消AI助手头像上传
     */
    public void cancelAiAvatar(String tempPath) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        uploadUtil.deleteTempFile(tempPath);
    }

    /**
     * 上传群聊头像到临时目录（第一阶段）
     */
    public String uploadRoomAvatarTemp(Long roomId, MultipartFile file) throws IOException {
        validateFile(file);
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        return uploadUtil.uploadToTemp(file);
    }

    /**
     * 确认群聊头像（第二阶段）
     */
    public String confirmRoomAvatar(Long roomId, String tempPath,
                                     String scheme, String serverName, int serverPort) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("房间不存在: " + roomId);
        }
        Room room = roomOpt.get();

        String oldAvatarUrl = room.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            uploadUtil.deleteFile(oldAvatarUrl);
        }

        int resolvedPort = resolvePort(serverPort);
        String relativeUrl;
        try {
            relativeUrl = uploadUtil.moveFromTemp(tempPath, AVATAR_SUB_DIR);
        } catch (IOException e) {
            throw new RuntimeException("移动群聊头像文件失败", e);
        }
        String absoluteUrl = uploadUtil.toAbsoluteFileUrl(relativeUrl, scheme, serverName, resolvedPort);

        room.setAvatarUrl(absoluteUrl);
        roomRepository.save(room);
        log.info("群聊头像确认成功 - roomId: {}, url: {}", roomId, absoluteUrl);

        return absoluteUrl;
    }

    /**
     * 取消群聊头像上传：删除临时文件
     */
    public void cancelRoomAvatar(String tempPath) {
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        uploadUtil.deleteTempFile(tempPath);
        log.info("群聊头像上传已取消，临时文件已删除: {}", tempPath);
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
