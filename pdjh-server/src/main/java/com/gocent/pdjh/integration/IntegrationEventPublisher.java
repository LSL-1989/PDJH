package com.gocent.pdjh.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 封装在内部核心服务里的公共广播器。
 * 供 `QueueManager` 或 `DoctorCallServiceImpl` 等叫号系统进行解耦调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationEventPublisher {

    private final IntegrationWebSocketHandler websocketHandler;
    private final com.gocent.pdjh.service.SystemActionLogService systemActionLogService;

    /**
     * 包装并发布通用事件给第三方系统
     */
    public void publishEvent(String eventType, Map<String, Object> dataMap) {
        log.debug("分发外部标准化事件 - Type: {}, Data: {}", eventType, dataMap);
        
        IntegrationEvent evt = IntegrationEvent.builder()
                .eventType(eventType)
                .data(dataMap)
                .build();
                
        // 调用底层 WebSocket 推送给外部厂家
        websocketHandler.broadcastEvent(evt);
        
        // P5: 记录外部推送日志
        systemActionLogService.recordLogAsync(
                "MSG_PUSH",
                "WebSocket_Broadcaster",
                "已推送集成事件: " + eventType,
                dataMap,
                true,
                null
        );
    }
}
