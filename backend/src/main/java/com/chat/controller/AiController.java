package com.chat.controller;

import com.chat.entity.AiAssistant;
import com.chat.entity.AiConversation;
import com.chat.entity.AiMessage;
import com.chat.service.AiAssistantService;
import com.chat.service.AiChatService;
import com.chat.service.AiConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/assistants")
    public ResponseEntity<Map<String, Object>> getAssistants(@RequestParam(required = false) Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("system", aiAssistantService.getSystemAssistant());
        result.put("user", userId != null ? aiAssistantService.getUserAssistants(userId) : List.of());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/assistants/{id}")
    public ResponseEntity<AiAssistant> getAssistant(@PathVariable Long id) {
        return aiAssistantService.getAssistantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assistants")
    public ResponseEntity<AiAssistant> createAssistant(@RequestBody Map<String, Object> request) {
        Long userId = parseLong(request.get("userId"));
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        AiAssistant assistant = new AiAssistant();
        assistant.setName((String) request.get("name"));
        assistant.setSystemPrompt((String) request.get("systemPrompt"));
        assistant.setBaseUrl((String) request.get("baseUrl"));
        assistant.setApiKey((String) request.get("apiKey"));
        assistant.setModel((String) request.get("model"));
        assistant.setTemperature(request.get("temperature") != null ? 
            Float.parseFloat(request.get("temperature").toString()) : 0.7f);
        assistant.setAvatarIcon((String) request.get("avatarIcon"));
        assistant.setAvatarColor((String) request.get("avatarColor"));
        
        return ResponseEntity.ok(aiAssistantService.createUserAssistant(userId, assistant));
    }

    @PutMapping("/assistants/{id}")
    public ResponseEntity<AiAssistant> updateAssistant(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Long userId = parseLong(request.get("userId"));
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        AiAssistant assistant = new AiAssistant();
        assistant.setName((String) request.get("name"));
        assistant.setSystemPrompt((String) request.get("systemPrompt"));
        assistant.setBaseUrl((String) request.get("baseUrl"));
        assistant.setApiKey((String) request.get("apiKey"));
        assistant.setModel((String) request.get("model"));
        assistant.setTemperature(request.get("temperature") != null ? 
            Float.parseFloat(request.get("temperature").toString()) : 0.7f);
        assistant.setAvatarIcon((String) request.get("avatarIcon"));
        assistant.setAvatarColor((String) request.get("avatarColor"));
        
        return ResponseEntity.ok(aiAssistantService.updateUserAssistant(userId, id, assistant));
    }

    @DeleteMapping("/assistants/{id}")
    public ResponseEntity<Void> deleteAssistant(@PathVariable Long id, @RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        aiAssistantService.deleteUserAssistant(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assistants/share")
    public ResponseEntity<AiAssistant> addByShareCode(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String shareCode = request.get("shareCode");
        
        if (userIdStr == null || shareCode == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long userId = Long.parseLong(userIdStr);
        return ResponseEntity.ok(aiAssistantService.copyAssistantByShareCode(userId, shareCode));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<AiConversation>> getConversations(@RequestParam Long assistantId, @RequestParam Long userId) {
        return ResponseEntity.ok(aiConversationService.getUserConversations(userId, assistantId));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<AiMessage>> getMessages(@RequestParam Long conversationId) {
        return ResponseEntity.ok(aiChatService.getConversationMessages(conversationId));
    }
    
    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
