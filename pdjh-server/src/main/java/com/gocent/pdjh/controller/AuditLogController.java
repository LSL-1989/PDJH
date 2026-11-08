package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 外部网关审计查询：读 gw_audit_log，供管理后台「外部网关审计查询」页。
 * 与 SystemActionLog（业务事件）、PatientFlowLog（患者轨迹）分开，本接口只查集成调用。
 */
@RestController
@RequestMapping("/AuditLog")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询网关调用记录。
     * 入参：endpointCode、isSuccess、startTime、endTime（yyyy-MM-dd HH:mm:ss）、
     * minDurationMs（≤0 不限）、keyword（请求/响应/错误信息）、page（从 1 起）、size。
     * 出参 data.list 为当前页，data.total 与 data.stats 为本次筛选的全量统计，不是当前页。
     */
    @PostMapping("/Query")
    public ResultData<Map<String, Object>> query(@RequestBody(required = false) Map<String, Object> params) {
        try {
            return ResultData.success(auditLogService.query(params), "查询成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 按与 Query 相同的筛选条件导出 CSV（含请求/响应），最多 2000 条。
     * 与 Query 分开：响应是文件文本而不是分页，并额外写入 sys_action_log（DATA_EXPORT）。
     * 出参 data.csv、data.count、data.truncated。
     */
    @PostMapping("/Export")
    public ResultData<Map<String, Object>> export(@RequestBody(required = false) Map<String, Object> params,
                                                   @RequestHeader(value = "X-User-No", required = false) String userNo,
                                                   HttpServletRequest request) {
        try {
            return ResultData.success(auditLogService.exportCsv(params, userNo, clientIp(request)), "导出成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
