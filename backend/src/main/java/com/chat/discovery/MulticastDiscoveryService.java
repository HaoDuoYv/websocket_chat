package com.chat.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class MulticastDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(MulticastDiscoveryService.class);

    @Value("${discovery.multicast-group:224.0.0.167}")
    private String multicastGroup;

    @Value("${discovery.multicast-port:9999}")
    private int multicastPort;

    @Value("${discovery.alias:WebSocket Chat Server}")
    private String alias;

    @Value("${server.port:8081}")
    private int serverPort;

    private static final String PROTOCOL = "ws";
    private static final String VERSION = "1.0.0";
    private static final int BROADCAST_INTERVAL_SECONDS = 30;

    private MulticastSocket multicastSocket;
    private InetAddress group;
    private NetworkInterface multicastNetworkInterface;
    private ScheduledExecutorService scheduler;
    private volatile int onlineUserCount = 0;
    private volatile boolean running = false;
    private volatile boolean started = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void start() {
        try {
            group = InetAddress.getByName(multicastGroup);
            multicastSocket = new MulticastSocket(multicastPort);
            multicastSocket.setReuseAddress(true);

            // Join multicast group on all suitable interfaces
            multicastNetworkInterface = findMulticastInterface();
            if (multicastNetworkInterface != null) {
                multicastSocket.joinGroup(new InetSocketAddress(group, multicastPort), multicastNetworkInterface);
                log.info("Joined multicast group {} on interface {}", multicastGroup, multicastNetworkInterface.getDisplayName());
            } else {
                multicastSocket.joinGroup(group);
                log.info("Joined multicast group {} (default interface)", multicastGroup);
            }

            running = true;
            started = true;

            // Start listener thread
            Thread listenerThread = new Thread(this::listen, "discovery-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Start periodic broadcast
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "discovery-broadcaster");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(this::broadcastServerInfo, 5, BROADCAST_INTERVAL_SECONDS, TimeUnit.SECONDS);

            log.info("Multicast discovery service started on {}:{} (group={})", getLocalIp(), multicastPort, multicastGroup);
        } catch (Exception e) {
            started = false;
            log.error("Failed to start multicast discovery service", e);
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        started = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                if (group != null) {
                    if (multicastNetworkInterface != null) {
                        multicastSocket.leaveGroup(new InetSocketAddress(group, multicastPort), multicastNetworkInterface);
                    } else {
                        multicastSocket.leaveGroup(group);
                    }
                }
                multicastSocket.close();
            } catch (IOException e) {
                log.error("Error closing multicast socket", e);
            }
        }
        log.info("Multicast discovery service stopped");
    }

    public boolean isStarted() {
        return started;
    }

    public void setOnlineUserCount(int count) {
        this.onlineUserCount = count;
    }

    private void listen() {
        final int MAX_RESTARTS = 5;
        int restartCount = 0;
        boolean messageProcessed = false;

        while (running) {
            try {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (running) {
                    multicastSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    InetAddress senderAddress = packet.getAddress();
                    int senderPort = packet.getPort();

                    log.debug("Received discovery message from {}: {}", senderAddress, message);

                    Map<String, Object> received = objectMapper.readValue(message, Map.class);
                    String type = (String) received.get("type");

                    if ("discovery-request".equals(type)) {
                        // Respond with server info directly to the sender
                        Map<String, Object> response = buildServerInfo();
                        response.put("type", "discovery-response");
                        byte[] responseBytes = objectMapper.writeValueAsBytes(response);
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseBytes, responseBytes.length, senderAddress, senderPort);
                        multicastSocket.send(responsePacket);
                        log.debug("Sent discovery response to {}:{}", senderAddress, senderPort);
                    }
                    messageProcessed = true;
                    restartCount = 0;
                }
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.error("Error in discovery listener", e);
                restartCount++;
                if (restartCount > MAX_RESTARTS) {
                    log.error("Discovery listener exceeded max restarts ({}), giving up", MAX_RESTARTS);
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void broadcastServerInfo() {
        try {
            Map<String, Object> info = buildServerInfo();
            info.put("type", "discovery-response");
            byte[] data = objectMapper.writeValueAsBytes(info);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, multicastPort);
            multicastSocket.send(packet);
            log.debug("Broadcasted server info: alias={}, users={}", alias, onlineUserCount);
        } catch (IOException e) {
            log.error("Error broadcasting server info", e);
        }
    }

    private Map<String, Object> buildServerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("alias", alias);
        info.put("ip", getLocalIp());
        info.put("port", serverPort);
        info.put("protocol", PROTOCOL);
        info.put("userCount", onlineUserCount);
        info.put("version", VERSION);
        return info;
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() || ni.isPointToPoint()) {
                    continue;
                }
                // Prefer IPv4, skip IPv6
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Error enumerating network interfaces", e);
        }
        return "127.0.0.1";
    }

    private NetworkInterface findMulticastInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }
                if (ni.supportsMulticast()) {
                    return ni;
                }
            }
        } catch (SocketException e) {
            log.error("Error finding multicast interface", e);
        }
        return null;
    }
}
