package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.service.MessageNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockMessageNotificationServiceImpl implements MessageNotificationService {

    @Override
    public void sendQueueReminder(String patientCode, String deptName, int waitCount, int estimatedMinutes) {
        log.info("【智慧医院消息网关-待办提醒】Mock发送: 患者 {} 准备前往 {}，前方还有 {} 人，预计等待 {} 分钟",
                patientCode, deptName, waitCount, estimatedMinutes);
        // FIXME: 对接医院真实的短信通道或微信服务号推送 API
    }

    @Override
    public void sendRoutingReminder(String patientCode, String completedItem, String nextDeptName) {
        if (nextDeptName != null) {
            log.info("【智慧医院消息网关-导诊提醒】Mock发送: 患者 {} 已完成 {}，下一站请前往 {}",
                    patientCode, completedItem, nextDeptName);
        } else {
            log.info("【智慧医院消息网关-完结提醒】Mock发送: 患者 {} 所有的体检项目已经做完啦！", patientCode);
        }
    }

    @Override
    public void sendEmergencyAlert(String patientCode, String originalDept, String message) {
        log.warn("【智慧医院消息网关-熔断告警】Mock发送: 患者 {} 关注，由于 {} 发生异动，{}", 
                patientCode, originalDept, message);
    }
}
