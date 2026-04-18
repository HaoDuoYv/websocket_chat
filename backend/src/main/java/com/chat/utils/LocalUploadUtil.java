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

/**
 * 本地文件上传工具类
 * 参考苍穹外卖项目实现
 */
@Data
@Slf4j
@AllArgsConstructor
public class LocalUploadUtil {

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
        return localProperties.getWebUrl() + "/" + dateDir + "/" + filename;
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
        return localProperties.getWebUrl() + "/" + targetDir + "/" + filename;
    }
}
