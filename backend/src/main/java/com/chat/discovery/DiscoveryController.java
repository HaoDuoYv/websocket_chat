package com.chat.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private static final String VERSION = "1.0.0";
    private static final String PROTOCOL = "ws";

    @Value("${discovery.alias:WebSocket Chat Server}")
    private String alias;

    @Value("${server.port:8081}")
    private int serverPort;

    private final MulticastDiscoveryService discoveryService;

    public DiscoveryController(MulticastDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("alias", alias);
        info.put("port", serverPort);
        info.put("protocol", PROTOCOL);
        info.put("version", VERSION);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        if (discoveryService.isStarted()) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        } else {
            return ResponseEntity.ok(Map.of("status", "unavailable"));
        }
    }
}
