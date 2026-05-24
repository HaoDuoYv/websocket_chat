package com.chat.utils;

import com.chat.properties.LocalProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地文件上传工具类
 * 使用 SHA-256 内容哈希命名，自动去重
 */
@Data
@Slf4j
@AllArgsConstructor
public class LocalUploadUtil {

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^(https?)://([^/:]+)(:\\d+)?(/.*)?$");

    private LocalProperties localProperties;

    public String uploadWithInfo(MultipartFile file, String subDir, String chatId, String senderId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        byte[] fileBytes = file.getBytes();
        String contentHash = sha256(fileBytes);
        String filename = contentHash + fileExtension;

        String targetDir = subDir != null ? subDir : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File dir = new File(absolutePath, targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(dir, filename);
        if (!destFile.exists()) {
            file.transferTo(destFile);
            log.info("文件保存（新建）: {}", destFile.getAbsolutePath());
        } else {
            log.info("文件已存在，跳过写入: {}", destFile.getAbsolutePath());
        }

        return buildRelativeFileUrl(targetDir, filename);
    }

    public String uploadToTemp(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        byte[] fileBytes = file.getBytes();
        String contentHash = sha256(fileBytes);
        String filename = contentHash + fileExtension;

        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File tempDir = new File(absolutePath, "temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File destFile = new File(tempDir, filename);
        if (!destFile.exists()) {
            file.transferTo(destFile);
            log.info("临时文件保存: {}", destFile.getAbsolutePath());
        }

        return "temp/" + filename;
    }

    public String moveFromTemp(String tempRelativePath, String targetSubDir) throws IOException {
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        String filename = tempRelativePath.substring(tempRelativePath.lastIndexOf('/') + 1);

        File srcFile = new File(absolutePath, tempRelativePath);
        File destDir = new File(absolutePath, targetSubDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destDir, filename);

        if (srcFile.exists()) {
            if (!destFile.exists()) {
                srcFile.renameTo(destFile);
                log.info("文件从temp移动到正式目录: {}", destFile.getAbsolutePath());
            } else {
                srcFile.delete();
                log.info("正式目录已存在同名文件，删除temp文件");
            }
        }

        return buildRelativeFileUrl(targetSubDir, filename);
    }

    public void deleteTempFile(String tempRelativePath) {
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File file = new File(absolutePath, tempRelativePath);
        if (file.exists()) {
            file.delete();
            log.info("删除临时文件: {}", file.getAbsolutePath());
        }
    }

    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        String cleanPath = relativePath;
        if (cleanPath.startsWith("/files/")) {
            cleanPath = cleanPath.substring(7);
        } else if (cleanPath.startsWith("files/")) {
            cleanPath = cleanPath.substring(6);
        }
        File file = new File(absolutePath, cleanPath);
        if (file.exists()) {
            file.delete();
            log.info("删除文件: {}", file.getAbsolutePath());
        }
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

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
