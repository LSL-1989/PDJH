package com.gocent.pdjh.integration;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/**
 * 标准化事件实体，专门供第三方厂家接收 JSON 原生报文。
 */
@Data
@Builder
public class IntegrationEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    private String eventType; // ENQUEUE_EVENT, CALL_EVENT, STATUS_CHANGE_EVENT, OVER_EVENT
    
    @Builder.Default
    private String timestamp = Instant.now().toString();
    
    // 事件承载的具体数据包 (JSON 宽表格式)
    private Object data;
}
