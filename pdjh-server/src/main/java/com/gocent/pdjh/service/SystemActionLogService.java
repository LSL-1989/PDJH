package com.gocent.pdjh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.SystemActionLog;
import com.gocent.pdjh.repository.SystemActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemActionLogService {

    private final SystemActionLogRepository systemActionLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 核心统一异步日志落点
     */
    @Async
    public void recordLogAsync(String logType, String operator, String actionDesc, Object payloadBody, boolean isSuccess, String errorMessage) {
        try {
            SystemActionLog actionLog = new SystemActionLog();
            actionLog.setLogType(logType);
            actionLog.setOperator(operator != null ? operator : "SYS");
            actionLog.setActionDesc(actionDesc);
            actionLog.setIsSuccess(isSuccess);
            actionLog.setErrorMessage(errorMessage);

            // 转换 Payload
            if (payloadBody != null) {
                if (payloadBody instanceof String) {
                    actionLog.setPayload((String) payloadBody);
                } else {
                    actionLog.setPayload(objectMapper.writeValueAsString(payloadBody));
                }
            }
            
            systemActionLogRepository.save(actionLog);
        } catch (Exception e) {
            log.error("保存审计系统事件失败: type={}, cause={}", logType, e.getMessage());
        }
    }

    /**
     * 定时任务：每天凌晨 2 点执行日志清理
     * 默认清理 30 天之前的陈旧日志，防止存储雪崩。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        log.info("[定时任务] 开始清理过期业务活动日志 (30天前)...");
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date cutoff = cal.getTime();
            
            systemActionLogRepository.deleteOlderThan(cutoff);
            log.info("[定时任务] 清理系统事件日志成功，清理截止时间: {}", cutoff);
        } catch (Exception e) {
            log.error("[定时任务] 清理系统事件日志失败: {}", e.getMessage(), e);
        }
    }
}
