package com.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String DATA_DIR = "chat_data";
    private final Map<String, List<Map<String, Object>>> messageCache = new ConcurrentHashMap<>();

    public MessageStorageService() {
        // 创建数据目录
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 保存消息到文件
     */
    public void saveMessage(String chatId, Map<String, Object> message) {
        try {
            List<Map<String, Object>> messages = getMessages(chatId);
            messages.add(message);
            
            // 保存到文件
            String fileName = DATA_DIR + "/" + chatId + ".json";
            objectMapper.writeValue(new File(fileName), messages);
            
            // 更新缓存
            messageCache.put(chatId, messages);
        } catch (IOException e) {
            System.err.println("保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取聊天的所有消息
     */
    public List<Map<String, Object>> getMessages(String chatId) {
        // 先从缓存获取
        if (messageCache.containsKey(chatId)) {
            return new ArrayList<>(messageCache.get(chatId));
        }
        
        // 从文件读取
        String fileName = DATA_DIR + "/" + chatId + ".json";
        File file = new File(fileName);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            List<Map<String, Object>> messages = objectMapper.readValue(
                file, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            messageCache.put(chatId, messages);
            return new ArrayList<>(messages);
        } catch (IOException e) {
            System.err.println("读取消息失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取聊天的最新消息（用于显示在列表中）
     */
    public Map<String, Object> getLastMessage(String chatId) {
        List<Map<String, Object>> messages = getMessages(chatId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
}
