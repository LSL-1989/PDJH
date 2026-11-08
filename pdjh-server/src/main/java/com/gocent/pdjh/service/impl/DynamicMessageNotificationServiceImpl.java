package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.integration.engine.IntegrationEngineService;
import com.gocent.pdjh.service.MessageNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态消息网关实现类。
 * 利用配置在 DB 的 `MESSAGE_BUS` 端点发散消息。
 */
@Service
@Primary
public class DynamicMessageNotificationServiceImpl implements MessageNotificationService {

    @Autowired
    private IntegrationEngineService integrationEngineService;

    // We assume there's a GwEndpoint with endpointCode = "SYS_MESSAGE_BUS"
    private static final String MSG_GATEWAY_CODE = "SYS_MESSAGE_BUS";

    @Override
    public void sendQueueReminder(String patientCode, String deptName, int waitCount, int estimatedMinutes) {
        Map<String, Object> params = new HashMap<>();
        params.put("patientCode", patientCode);
        params.put("templateId", "TPL_QUEUE_REMINDER");
        params.put("deptName", deptName);
        params.put("waitCount", waitCount);
        params.put("estimatedMinutes", estimatedMinutes);

        integrationEngineService.executeGatewayCall(MSG_GATEWAY_CODE, params);
        System.out.println("Dynamic MsgBus: Sent Queue Reminder to " + patientCode);
    }

    @Override
    public void sendRoutingReminder(String patientCode, String completedItem, String nextDeptName) {
        Map<String, Object> params = new HashMap<>();
        params.put("patientCode", patientCode);
        params.put("templateId", "TPL_ROUTING");
        params.put("completedItem", completedItem);
        params.put("nextDeptName", nextDeptName == null ? "回家" : nextDeptName);

        integrationEngineService.executeGatewayCall(MSG_GATEWAY_CODE, params);
        System.out.println("Dynamic MsgBus: Sent Routing Reminder to " + patientCode);
    }

    @Override
    public void sendEmergencyAlert(String patientCode, String originalDept, String message) {
        Map<String, Object> params = new HashMap<>();
        params.put("patientCode", patientCode);
        params.put("templateId", "TPL_EMERGENCY");
        params.put("originalDept", originalDept);
        params.put("message", message);

        integrationEngineService.executeGatewayCall(MSG_GATEWAY_CODE, params);
        System.out.println("Dynamic MsgBus: Sent Emergency Alert to " + patientCode + " -> " + message);
    }
}
