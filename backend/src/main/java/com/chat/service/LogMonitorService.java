package com.chat.service;

import com.chat.config.AdminConfig;
import com.chat.vo.LogLineVO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志监控服务
 */
@Service
public class LogMonitorService {

    @Autowired
    private AdminConfig adminConfig;

    private final List<LogLineVO> recentLogs = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG_LINES = 1000;
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(INFO|ERROR|WARN|DEBUG).*"
    );

    private WatchService watchService;
    private Path logDirPath;
    private long lastReadLine = 0;

    @PostConstruct
    public void init() {
        try {
            // 初始化日志目录
            String logPath = adminConfig.getLogPath();
            logDirPath = Paths.get(logPath);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }

            // 启动日志监控
            startLogMonitoring();
        } catch (Exception e) {
            System.err.println("日志监控初始化失败: " + e.getMessage());
        }
    }

    /**
     * 启动日志监控
     */
    private void startLogMonitoring() {
        new Thread(() -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                logDirPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                // 初始读取日志文件
                readLogFile();

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            readLogFile();
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                System.err.println("日志监控异常: " + e.getMessage());
            }
        }, "LogMonitor-Thread").start();
    }

    /**
     * 读取日志文件
     */
    private void readLogFile() {
        try {
            // 查找最新的日志文件
            Path latestLogFile = findLatestLogFile();
            if (latestLogFile == null) {
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(latestLogFile)) {
                String line;
                long lineNumber = 0;
                List<LogLineVO> newLogs = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber <= lastReadLine) {
                        continue;
                    }

                    LogLineVO logLine = parseLogLine(line, lineNumber);
                    if (logLine != null) {
                        newLogs.add(logLine);
                    }
                }

                lastReadLine = lineNumber;

                // 添加到缓存
                if (!newLogs.isEmpty()) {
                    recentLogs.addAll(newLogs);
                    // 限制缓存大小
                    while (recentLogs.size() > MAX_LOG_LINES) {
                        recentLogs.remove(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 查找最新的日志文件
     */
    private Path findLatestLogFile() throws IOException {
        if (!Files.exists(logDirPath)) {
            return null;
        }

        return Files.list(logDirPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".log"))
                .max((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .orElse(null);
    }

    /**
     * 解析日志行
     */
    private LogLineVO parseLogLine(String line, long lineNumber) {
        LogLineVO logLine = new LogLineVO();
        logLine.setLineNumber(lineNumber);
        logLine.setContent(line);

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            logLine.setTimestamp(matcher.group(1));
            logLine.setLevel(matcher.group(2));
        } else {
            logLine.setLevel("INFO");
            logLine.setTimestamp("");
        }

        return logLine;
    }

    /**
     * 获取最近的日志
     */
    public List<LogLineVO> getRecentLogs(int limit) {
        int size = recentLogs.size();
        if (limit >= size) {
            return new ArrayList<>(recentLogs);
        }
        return new ArrayList<>(recentLogs.subList(size - limit, size));
    }

    /**
     * 获取所有缓存的日志
     */
    public List<LogLineVO> getAllLogs() {
        return new ArrayList<>(recentLogs);
    }

    /**
     * 清空日志缓存
     */
    public void clearLogs() {
        recentLogs.clear();
        lastReadLine = 0;
    }
}
