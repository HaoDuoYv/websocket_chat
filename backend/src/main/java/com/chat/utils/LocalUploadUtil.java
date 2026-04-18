package com.chat.utils;

import com.chat.properties.LocalProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地文件上传工具类
 * 参考苍穹外卖项目实现
 */
@Data
@Slf4j
@AllArgsConstructor
public class LocalUploadUtil {

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^(https?)://([^/:]+)(:\\d+)?(/.*)?$");

    private LocalProperties localProperties;

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件访问URL（相对路径，如 /files/20260412/uuid.jpg）
     * @throws IOException IO异常
     */
    public String upload(MultipartFile file) throws IOException {
        // 获取原始文件名并生成新文件名（UUID + 扩展名）
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + fileExtension;

        // 按日期创建子目录（如 20260412）
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 将相对路径转换为绝对路径，基于项目工作目录
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File dir = new File(absolutePath, dateDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        File destFile = new File(dir, filename);
        log.info("文件保存路径：{}", destFile.getAbsolutePath());
        file.transferTo(destFile);

        // 返回相对路径URL（如 /files/20260412/uuid.jpg）
        return buildRelativeFileUrl(dateDir, filename);
    }

    /**
     * 上传文件（带自定义子目录）
     *
     * @param file      上传的文件
     * @param subDir    子目录名
     * @param chatId    聊天ID
     * @param senderId  发送者ID
     * @return 文件访问URL
     * @throws IOException IO异常
     */
    public String uploadWithInfo(MultipartFile file, String subDir, String chatId, String senderId) throws IOException {
        // 获取原始文件名并生成新文件名（UUID + 扩展名）
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + fileExtension;

        // 使用指定的子目录或按日期创建
        String targetDir = subDir != null ? subDir : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 将相对路径转换为绝对路径
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File dir = new File(absolutePath, targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        File destFile = new File(dir, filename);
        log.info("文件保存路径：{}，聊天ID：{}，发送者：{}", destFile.getAbsolutePath(), chatId, senderId);
        file.transferTo(destFile);

        // 返回相对路径URL
        return buildRelativeFileUrl(targetDir, filename);
    }

    private String buildRelativeFileUrl(String directory, String filename) {
        return localProperties.getWebUrl() + "/" + directory + "/" + filename;
    }

    public String toAbsoluteFileUrl(String fileUrl, String scheme, String serverName, int serverPort) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return fileUrl;
        }

        int resolvedPort = serverPort > 0 ? serverPort : 8081;
        String normalizedPath = normalizeFileUrlPath(fileUrl, scheme, serverName, resolvedPort);
        StringBuilder builder = new StringBuilder();
        builder.append(scheme).append("://").append(serverName);
        if (!(("http".equalsIgnoreCase(scheme) && resolvedPort == 80)
                || ("https".equalsIgnoreCase(scheme) && resolvedPort == 443))) {
            builder.append(":").append(resolvedPort);
        }
        builder.append(normalizedPath);
        return builder.toString();
    }

    private String normalizeFileUrlPath(String fileUrl, String scheme, String serverName, int serverPort) {
        Matcher matcher = HTTP_URL_PATTERN.matcher(fileUrl);
        if (matcher.matches()) {
            String existingScheme = matcher.group(1);
            String existingHost = matcher.group(2);
            String existingPath = matcher.group(4);

            boolean sameHost = existingHost != null && existingHost.equalsIgnoreCase(serverName);
            boolean sameScheme = existingScheme != null && existingScheme.equalsIgnoreCase(scheme);
            if (sameHost && sameScheme) {
                return existingPath == null || existingPath.isBlank() ? "/" : existingPath;
            }
            return fileUrl;
        }

        if (fileUrl.startsWith("http//") || fileUrl.startsWith("https//")) {
            String fixedUrl = fileUrl.replaceFirst("^http//", "http://")
                    .replaceFirst("^https//", "https://");
            return normalizeFileUrlPath(fixedUrl, scheme, serverName, serverPort);
        }

        return fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl;
    }
}
