package com.gocent.pdjh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 专门供第三方厂家接入的原生 WebSocket Handler
 * 路径：ws://host:port/api/ws/integration?appKey={厂家标识}（context-path 为 /api 时）
 */
@Slf4j
@Component
public class IntegrationWebSocketHandler extends TextWebSocketHandler {

    // 存储当前建立的原始 Session (SessionId -> Session)
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        String appKey = "anonymous";
        if (query != null && query.contains("appKey=")) {
            appKey = query.split("appKey=")[1].split("&")[0];
        }
        
        log.info("【第三方集成】新的原生 WS 客户端已连接. AppKey: {}, SessionId: {}", appKey, session.getId());
        SESSIONS.put(session.getId(), session);
        
        // 可选：下发一条欢迎连接事件
        IntegrationEvent welcomeEvent = IntegrationEvent.builder()
                .eventType("CONNECTION_ACCEPTED")
                .data(Map.of("message", "Welcome to PDJH Integration Bus", "appKey", appKey))
                .build();
        sendMessage(session, welcomeEvent);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 外部厂家通常仅做事件订阅（只读），若未来要允许他们双向下发叫号指令可以从此处解析处理。
        log.debug("收到厂商心跳或上行消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("【第三方集成】原生 WS 客户端已断开. SessionId: {}, Status: {}", session.getId(), status.getCode());
        SESSIONS.remove(session.getId());
    }

    /**
     * 核心开放方法：供内部的事件发布者调用，进行全网全量多宿主广播
     */
    public void broadcastEvent(IntegrationEvent event) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        try {
            String jsonPayload = mapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(jsonPayload);
            SESSIONS.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("WS 广播向外部客户端发送失败. Session: {}", session.getId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("系列化第三方事件报文失败", e);
        }
    }

    private void sendMessage(WebSocketSession session, IntegrationEvent event) {
        try {
            String jsonPayload = mapper.writeValueAsString(event);
            session.sendMessage(new TextMessage(jsonPayload));
        } catch (IOException e) {
            log.warn("向自身下发欢迎报文失败", e);
        }
    }
}
