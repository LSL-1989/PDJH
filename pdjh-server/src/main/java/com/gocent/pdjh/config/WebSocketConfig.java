package com.gocent.pdjh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.gocent.pdjh.integration.IntegrationWebSocketHandler;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final IntegrationWebSocketHandler integrationWebSocketHandler;

    public WebSocketConfig(IntegrationWebSocketHandler integrationWebSocketHandler) {
        this.integrationWebSocketHandler = integrationWebSocketHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用内存中基于简单主题的消息代理，并将响应发回客户端（对应主题前缀）
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置客户端向服务端发送请求的目的地前缀，即所有的客户端消息需以 /app 开头
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@org.springframework.lang.NonNull StompEndpointRegistry registry) {
        // 注册 STOMP 端点，供各类前端握手连接（拒绝通配请求防御长连接洪水泄漏）
        registry.addEndpoint("/pdjh-ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://*.hospital.local", "https://*.hospital.local")
                .withSockJS();
        // 原生 WebSocket STOMP（无 SockJS）：供 HJ叫号器 WPF ClientWebSocket 连接
        registry.addEndpoint("/pdjh-ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void registerWebSocketHandlers(@org.springframework.lang.NonNull WebSocketHandlerRegistry registry) {
        // 注册给第三方厂家对接的原生 WebSocket Endpoint，完全放开跨域限制
        registry.addHandler(integrationWebSocketHandler, "/ws/integration")
                .setAllowedOrigins("*");
    }
}
