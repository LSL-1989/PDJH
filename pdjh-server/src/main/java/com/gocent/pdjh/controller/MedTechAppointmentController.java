package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.MedTechSlot;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.service.MedTechQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * P1-1e：医技科室预约时段管理接口 + 叫号调度接口
 */
@Slf4j
@RestController
@RequestMapping("/medtech")
public class MedTechAppointmentController {

    private final MedTechQueueService medTechQueueService;
    private final DepartmentRepository departmentRepository;

    public MedTechAppointmentController(
            MedTechQueueService medTechQueueService,
            DepartmentRepository departmentRepository) {
        this.medTechQueueService = medTechQueueService;
        this.departmentRepository = departmentRepository;
    }

    // ------------------------------------------------------------------
    // 预约时段查询
    // ------------------------------------------------------------------

    /**
     * GET /api/medtech/slots?deptId=XXX&date=2026-04-01
     * 查询指定科室某日全部时段（含已满、维护中的）
     */
    @GetMapping("/slots")
    public ResultData<Map<String, Object>> getSlots(
            @RequestParam String deptId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) date = LocalDate.now();
        List<MedTechSlot> slots = medTechQueueService.getAllSlots(deptId, date);

        // 计算汇总
        int totalCapacity = slots.stream().mapToInt(MedTechSlot::getCapacity).sum();
        int totalBooked   = slots.stream().mapToInt(MedTechSlot::getBooked).sum();

        Optional<Department> dept = departmentRepository.findById(deptId);
        Map<String, Object> result = new HashMap<>();
        result.put("slots", slots);
        result.put("totalCapacity", totalCapacity);
        result.put("totalBooked", totalBooked);
        result.put("remaining", totalCapacity - totalBooked);
        result.put("deptName", dept.map(Department::getName).orElse(deptId));
        result.put("date", date.toString());
        return ResultData.success(result, "获取成功");
    }

    /**
     * GET /api/medtech/slots/available?deptId=XXX&date=2026-04-01
     * 查询仍有余量的可预约时段（供患者端选择）
     */
    @GetMapping("/slots/available")
    public ResultData<List<MedTechSlot>> getAvailableSlots(
            @RequestParam String deptId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResultData.success(medTechQueueService.getAvailableSlots(deptId, date), "获取成功");
    }

    /**
     * POST /api/medtech/appointment/book
     * 患者预约某时段
     * Body: { "deptId": "xxx", "date": "2026-04-01", "timeSlot": "09:00",
     *         "patientCode": "P001", "patientName": "张三" }
     */
    @PostMapping("/appointment/book")
    public ResultData<Void> bookAppointment(@RequestBody Map<String, Object> body) {
        String deptId     = (String) body.get("deptId");
        String dateStr    = (String) body.get("date");
        String timeSlot   = (String) body.get("timeSlot");

        if (deptId == null || dateStr == null || timeSlot == null) {
            return ResultData.fail(400, "deptId / date / timeSlot 不能为空");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResultData.fail(400, "日期格式错误，请使用 yyyy-MM-dd");
        }

        boolean ok = medTechQueueService.bookSlot(deptId, date, timeSlot);
        if (ok) {
            log.info("【预约成功】科室={}, 时段={}, 患者={}", deptId, timeSlot, body.get("patientCode"));
            return ResultData.success(null, "预约成功，请准时到检");
        } else {
            return ResultData.fail(400, "该时段配额已满，请选择其他时段");
        }
    }

    // ------------------------------------------------------------------
    // 叫号调度（后台护士站触发）
    // ------------------------------------------------------------------

    /**
     * POST /api/medtech/callNext?deptId=XXX
     * 叫下一个医技患者（由护士工作站或前端自动调用）
     */
    @PostMapping("/callNext")
    public ResultData<Map<String, Object>> callNext(@RequestParam String deptId) {
        log.info("【医技叫号】科室 {} 请求叫号", deptId);
        CallList next = medTechQueueService.selectNextPatient(deptId);
        if (next == null) {
            return ResultData.fail(404, "当前无等待患者");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("callListId", next.getId());
        data.put("patientName", next.getPatientName());
        data.put("showNo", next.getShowNo() != null ? next.getShowNo() : "");
        data.put("sourceType", next.getSourceType() != null ? next.getSourceType() : "");
        return ResultData.success(data, "叫号成功");
    }

    // ------------------------------------------------------------------
    // 配置工具：批量初始化某科室某日的时段配额
    // ------------------------------------------------------------------

    /**
     * POST /api/medtech/slots/init
     * 管理员批量初始化某科室某天的时段（如 08:00-17:00，每30分钟一档）
     * Body: { "deptId":"xxx", "date":"2026-04-01", "startTime":"08:00",
     *         "endTime":"17:00", "intervalMin":30, "capacity":3 }
     */
    @PostMapping("/slots/init")
    public ResultData<Map<String, Object>> initSlots(@RequestBody Map<String, Object> body) {
        String deptId = (String) body.get("deptId");
        String dateStr = (String) body.get("date");
        if (deptId == null || dateStr == null) {
            return ResultData.fail(400, "deptId / date 不能为空");
        }
        int startH = parseTime((String) body.getOrDefault("startTime", "08:00"), true);
        int startM = parseTime((String) body.getOrDefault("startTime", "08:00"), false);
        int endH = parseTime((String) body.getOrDefault("endTime", "17:00"), true);
        int endM = parseTime((String) body.getOrDefault("endTime", "17:00"), false);
        int interval = body.containsKey("intervalMin") ? Integer.parseInt(body.get("intervalMin").toString()) : 30;
        int capacity = body.containsKey("capacity") ? Integer.parseInt(body.get("capacity").toString()) : 2;

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResultData.fail(400, "日期格式错误，请使用 yyyy-MM-dd");
        }

        int created = medTechQueueService.createEmptySlotsIfAbsent(
                deptId, date, startH, startM, endH, endM, interval, capacity);

        Map<String, Object> data = new HashMap<>();
        data.put("created", created);
        data.put("date", dateStr);
        return ResultData.success(data, String.format("共初始化 %d 个新时段", created));
    }

    @PutMapping("/slots/{id}")
    public ResultData<MedTechSlot> updateSlot(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = medTechQueueService.updateSlot(id, body);
        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResultData.fail(404, String.valueOf(result.get("message")));
        }
        return ResultData.success((MedTechSlot) result.get("slot"), "保存成功");
    }

    @PatchMapping("/slots/{id}/enabled")
    public ResultData<MedTechSlot> toggleSlot(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean enabled = body.get("enabled") == null || Boolean.parseBoolean(body.get("enabled").toString());
        Map<String, Object> result = medTechQueueService.setSlotEnabled(id, enabled);
        if (Boolean.FALSE.equals(result.get("success"))) {
            return ResultData.fail(404, String.valueOf(result.get("message")));
        }
        return ResultData.success((MedTechSlot) result.get("slot"), enabled ? "已启用" : "已暂停");
    }

    private int parseTime(String time, boolean isHour) {
        if (time == null || !time.contains(":")) return isHour ? 8 : 0;
        String[] parts = time.split(":");
        return isHour ? Integer.parseInt(parts[0]) : Integer.parseInt(parts[1]);
    }
}
