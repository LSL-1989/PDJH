package com.gocent.pdjh.integration.controller;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.service.ExamRoutingService;
import com.gocent.pdjh.service.IntegrationConfigService;
import com.gocent.pdjh.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 院方主动推送槽：LIS/RIS 检查完成、HIS 挂号入队。
 * 与 {@code /integration/webhook/{hookId}} 的配置化解析入口并存；本接口字段固定，供未配置 hook 的系统直接调用。
 * 消费者：医院 LIS、RIS、HIS。
 */
@Slf4j
@RestController
@RequestMapping("/integration/webhook")
@RequiredArgsConstructor
public class PassiveIntegrationController {

    private final ExamRoutingService examRoutingService;
    private final QueueService queueService;
    private final OfficeRoomRepository officeRoomRepository;
    private final IntegrationConfigService integrationConfigService;

    /**
     * 第三方检查系统推送进度。state=4 表示该项已完成，回写体检明细并派下一站。
     * 入参：patientCode、itemCode（字典 id 或项目名称）、state。
     * 成功返回 SUCCESS；缺参或找不到项目返回 FAIL:原因，便于院方重试。
     */
    @PostMapping("/exam/status")
    public String receiveExamStatusUpdate(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String denied = integrationConfigService.webhookDenyReason("EXAM_STATUS", clientIp(request));
        if (denied != null) {
            return "FAIL:" + denied;
        }
        String patientCode = str(payload, "patientCode", "PatientCode", "patientId");
        String itemCode = str(payload, "itemCode", "itemId", "ItemCode", "itemName");
        Integer state = intVal(first(payload, "state", "State", "status"));
        log.info("【被动 Webhook】检查进度：患者 {} 项目 {} 状态 {}", patientCode, itemCode, state);
        try {
            String error = examRoutingService.applyExternalExamStatus(patientCode, itemCode, state);
            return error == null ? "SUCCESS" : "FAIL:" + error;
        } catch (RuntimeException ex) {
            log.error("【被动 Webhook】检查进度处理失败", ex);
            return "FAIL:" + ex.getMessage();
        }
    }

    /**
     * HIS 推送挂号。缺 patientCode，或既无诊室也无科室时返回 FAIL，不再空成功。
     * 只给科室时，落到该科室第一个开放诊室，患者状态为未签到（UNARRIVED）。
     */
    @PostMapping("/his/register")
    public String receiveHisRegistration(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String denied = integrationConfigService.webhookDenyReason("HIS_REGISTER", clientIp(request));
        if (denied != null) {
            return "FAIL:" + denied;
        }
        String patientCode = str(payload, "patientCode", "PatientCode", "patientId");
        String patientName = str(payload, "patientName", "PatientName", "name");
        String deptId = str(payload, "deptId", "deptID", "DeptID", "departmentCode");
        String officeRoomId = str(payload, "officeRoomId", "roomId", "OfficeRoomId", "RoomId");
        if (patientCode == null) {
            return "FAIL:patientCode 不能为空";
        }
        if (officeRoomId == null) {
            officeRoomId = firstOpenRoom(deptId);
        }
        if (officeRoomId == null) {
            return "FAIL:需要 officeRoomId 或能解析到诊室的 deptId";
        }
        EnqueueRequest req = new EnqueueRequest();
        req.setPatientCode(patientCode);
        req.setPatientName(patientName != null ? patientName : patientCode);
        req.setDeptID(deptId);
        req.setOfficeRoomId(officeRoomId);
        req.setShowNo(str(payload, "showNo", "sequenceNo", "queueNo"));
        req.setSourceType(str(payload, "sourceType", "SourceType"));
        req.setSex(str(payload, "sex", "Sex"));
        req.setAgeStr(str(payload, "ageStr", "age", "AgeStr"));
        Object direct = first(payload, "requireSignIn", "RequireSignIn");
        boolean requireSignIn = direct == null || Boolean.parseBoolean(direct.toString());
        req.setRequireSignIn(requireSignIn);
        try {
            queueService.enqueue(req);
            log.info("【被动 Webhook】HIS 挂号已入队 patientCode={} room={}", patientCode, officeRoomId);
            return "SUCCESS";
        } catch (RuntimeException ex) {
            log.error("【被动 Webhook】HIS 挂号入队失败 patientCode={}", patientCode, ex);
            return "FAIL:" + ex.getMessage();
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String firstOpenRoom(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            return null;
        }
        return officeRoomRepository.findByDeptId(deptId).stream()
                .filter(r -> r.getState() == null || r.getState() == 1)
                .map(r -> r.getId())
                .findFirst()
                .orElse(null);
    }

    private static Object first(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && payload.get(key) != null) {
                return payload.get(key);
            }
        }
        return null;
    }

    private static String str(Map<String, Object> payload, String... keys) {
        Object value = first(payload, keys);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
