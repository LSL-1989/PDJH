package com.gocent.pdjh.integration.webhook;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.IntegrationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 外部第三方系统（如LIS/PACS）主动向本系统推送数据的统一回调入口
 */
@RestController
@RequestMapping("/integration/webhook")
public class WebhookIntegrationController {

    @Autowired
    private WebhookDispatcherService webhookDispatcherService;

    @Autowired
    private com.gocent.pdjh.service.SysConfigService sysConfigService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.gocent.pdjh.service.QueueService queueService;

    @Autowired
    private IntegrationConfigService integrationConfigService;

    @PostMapping("/{hookId}")
    public ResultData<Object> receiveWebhook(@PathVariable String hookId, @RequestBody String payload, HttpServletRequest request) {
        String denied = integrationConfigService.webhookDenyReason(hookId, clientIp(request));
        if (denied != null) {
            return ResultData.fail(403, denied);
        }
        try {
            List<Map<String, Object>> extractedData = webhookDispatcherService.dispatchAndParse(hookId, payload);
            
            // P1-B: 读取 HIS缴费联动延迟参数 (paymentSyncDelaySec)
            int delaySec = 0;
            try {
                com.gocent.pdjh.entity.SysConfig config = sysConfigService.getConfigByKey("RULE_MEDICAL_TECH");
                if (config != null && config.getConfigValue() != null) {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(config.getConfigValue());
                    if (root.has("global") && root.get("global").has("paymentSyncDelaySec")) {
                        delaySec = root.get("global").get("paymentSyncDelaySec").asInt(0);
                    }
                }
            } catch (Exception e) {
                System.err.println("读取 RULE_MEDICAL_TECH 配置失败，将使用无延迟同步: " + e.getMessage());
            }
            
            Runnable enqueueTask = () -> processEnqueue(extractedData);

            if (delaySec > 0) {
                System.out.println("【患者缴费同步】已接收来自 HIS 的数据更新，根据规则配置，将于 " + delaySec + " 秒后写入排队表。");
                final int finalDelaySec = delaySec;
                new Thread(() -> {
                    try {
                        Thread.sleep(finalDelaySec * 1000L);
                        enqueueTask.run();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                enqueueTask.run();
            }
            
            return ResultData.success(extractedData, "Webhook received and scheduled (Delay: " + delaySec + "s)");
        } catch (Exception e) {
            System.err.println("Webhook parsing failed for hookId: " + hookId + " - " + e.getMessage());
            return ResultData.fail(500, "Webhook processing failed: " + e.getMessage());
        }
    }

    private void processEnqueue(List<Map<String, Object>> extractedData) {
        if (extractedData == null) return;
        for (Map<String, Object> row : extractedData) {
            try {
                EnqueueRequest req = new EnqueueRequest();
                req.setPatientCode(str(row, "patientCode", "patientId", "PatientCode"));
                req.setPatientName(str(row, "patientName", "PatientName", "name"));
                req.setDeptID(str(row, "deptId", "deptID", "DeptID", "departmentCode"));
                req.setOfficeRoomId(str(row, "officeRoomId", "roomId", "OfficeRoomId", "RoomId"));
                req.setShowNo(str(row, "showNo", "sequenceNo", "queueNo"));
                req.setSourceType(str(row, "sourceType", null, null));
                req.setSex(str(row, "sex", "Sex"));
                req.setAgeStr(str(row, "ageStr", "age", "AgeStr"));
                req.setDoctorID(str(row, "doctorID", "doctorId", "doctorNo"));
                req.setSflbbigtype(str(row, "sflbbigtype", "drugCategory"));
                Object signIn = row.get("requireSignIn");
                if (signIn != null) {
                    req.setRequireSignIn(Boolean.parseBoolean(signIn.toString()));
                }
                if (req.getPatientCode() != null && req.getDeptID() != null) {
                    queueService.enqueue(req);
                }
            } catch (Exception ex) {
                System.err.println("Webhook enqueue failed: " + ex.getMessage());
            }
        }
    }

    private static String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (k != null && m.get(k) != null) return m.get(k).toString();
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
