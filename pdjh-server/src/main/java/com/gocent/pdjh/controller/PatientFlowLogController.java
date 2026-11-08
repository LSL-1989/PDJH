package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.PatientFlowLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 患者全流程操作日志：供后台列表与时间轴抽屉
 * 与 SystemActionLog（技术事件）/ NurseActionLog（护士干预）不同，本接口按患者轨迹聚合
 */
@RestController
@RequestMapping("/PatientFlowLog")
@RequiredArgsConstructor
public class PatientFlowLogController {

    private final PatientFlowLogService patientFlowLogService;

    /**
     * 分页查询流程日志
     * body/query：pageindex、pagesize、keyword、eventType、patientCode、callListId、startTime、endTime
     */
    @PostMapping("/Query")
    public ResultData<Map<String, Object>> query(@RequestBody(required = false) Map<String, Object> body) {
        int pageindex = intVal(body, "pageindex", "pageIndex", 1);
        int pagesize = intVal(body, "pagesize", "pageSize", 20);
        String keyword = str(body, "keyword");
        String eventType = str(body, "eventType");
        String patientCode = str(body, "patientCode");
        Integer callListId = intOrNull(body, "callListId");
        Date startTime = dateVal(body, "startTime");
        Date endTime = dateVal(body, "endTime");
        return ResultData.success(
                patientFlowLogService.queryPage(pageindex, pagesize, keyword, eventType,
                        patientCode, callListId, startTime, endTime),
                "获取成功");
    }

    /**
     * 按排队单时间轴（节点升序）
     * body：{ "callListId": 123 }
     */
    @PostMapping("/TimelineByCallList")
    public ResultData<List<Map<String, Object>>> timelineByCallList(@RequestBody Map<String, Object> body) {
        Integer callListId = intOrNull(body, "callListId");
        if (callListId == null) {
            return ResultData.fail(400, "callListId 不能为空");
        }
        return ResultData.success(patientFlowLogService.timelineByCallListId(callListId), "获取成功");
    }

    /**
     * 按患者编号时间轴；可选 startTime/endTime
     */
    @PostMapping("/TimelineByPatient")
    public ResultData<List<Map<String, Object>>> timelineByPatient(@RequestBody Map<String, Object> body) {
        String patientCode = str(body, "patientCode");
        if (patientCode == null || patientCode.isBlank()) {
            return ResultData.fail(400, "patientCode 不能为空");
        }
        return ResultData.success(
                patientFlowLogService.timelineByPatientCode(patientCode,
                        dateVal(body, "startTime"), dateVal(body, "endTime")),
                "获取成功");
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String v = body.get(key).toString().trim();
        return v.isEmpty() ? null : v;
    }

    private static int intVal(Map<String, Object> body, String k1, String k2, int def) {
        Integer v = intOrNull(body, k1);
        if (v == null) {
            v = intOrNull(body, k2);
        }
        return v != null ? v : def;
    }

    private static Integer intOrNull(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null || body.get(key).toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(body.get(key).toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Date dateVal(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof Date) {
            return (Date) v;
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.length() <= 10) {
                return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
            }
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
