package com.gocent.pdjh.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationEngine {
    private static final Logger log = LoggerFactory.getLogger(NotificationEngine.class);
    
    // Concurrent map to hold open SSE connections mapped by patient/client ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE client connection with an infinite timeout.
     */
    public SseEmitter subscribe(String clientId) {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(clientId);
        });
        emitter.onError((e) -> emitters.remove(clientId));

        try {
            // Send initial connection heartbeat
            emitter.send(SseEmitter.event().name("CONNECT").data("Connected successfully"));
        } catch (IOException e) {
            emitters.remove(clientId);
        }

        return emitter;
    }

    /**
     * Pushes a real-time event directly to a specific connected client.
     */
    public void pushToClient(String clientId, String eventName, Object payload) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                log.warn("Failed to push to client {}, removing connection", clientId);
                emitters.remove(clientId);
            }
        }
    }
    
    /**
     * Broadcasts an event to all connected big screens (department level).
     */
    public void broadcast(String eventName, Object payload) {
        emitters.forEach((id, emitter) -> {
            if (id.startsWith("SCREEN_")) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(payload));
                } catch (IOException e) {
                    emitters.remove(id);
                }
            }
        });
    }
}
