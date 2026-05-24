package com.chat.config;

import com.chat.entity.FileRecord;
import com.chat.properties.LocalProperties;
import com.chat.repository.FileRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class OrphanFileCleaner {

    @Autowired
    private LocalProperties localProperties;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    /**
     * 每小时清理 temp 目录中超过 1 小时的文件
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanTempFiles() {
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();
        File tempDir = new File(absolutePath, "temp");
        if (!tempDir.exists()) return;

        long threshold = System.currentTimeMillis() - 3600000; // 1 hour
        File[] files = tempDir.listFiles();
        if (files == null) return;

        int cleaned = 0;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < threshold) {
                file.delete();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("清理过期临时文件: {} 个", cleaned);
        }
    }

    /**
     * 每天凌晨 3 点清理孤儿文件（磁盘上存在但数据库无引用的文件）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOrphanFiles() {
        String absolutePath = System.getProperty("user.dir") + File.separator + localProperties.getLocalUrl();

        // 收集数据库中所有文件 URL 对应的文件名
        Set<String> dbFileNames = new HashSet<>();
        List<FileRecord> allRecords = fileRecordRepository.findAll();
        for (FileRecord record : allRecords) {
            String url = record.getFileUrl();
            if (url != null && !url.isBlank()) {
                // 从 URL 提取文件名: .../files/avatars/abc.png → abc.png
                String name = url.substring(url.lastIndexOf('/') + 1);
                dbFileNames.add(name);
            }
        }

        // 扫描 uploads 目录（排除 temp 和 avatars）
        int cleaned = cleanDirectory(new File(absolutePath), dbFileNames, "temp", "avatars");

        // 单独扫描 avatars 目录
        File avatarsDir = new File(absolutePath, "avatars");
        if (avatarsDir.exists()) {
            cleaned += cleanDirectory(avatarsDir, dbFileNames);
        }

        if (cleaned > 0) {
            log.info("清理孤儿文件: {} 个", cleaned);
        }
    }

    private int cleanDirectory(File dir, Set<String> dbFileNames, String... excludeSubDirs) {
        if (!dir.exists()) return 0;
        int cleaned = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                boolean excluded = false;
                for (String ex : excludeSubDirs) {
                    if (file.getName().equals(ex)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    cleaned += cleanDirectory(file, dbFileNames);
                }
            } else if (file.isFile()) {
                if (!dbFileNames.contains(file.getName())) {
                    file.delete();
                    cleaned++;
                }
            }
        }
        return cleaned;
    }
}
