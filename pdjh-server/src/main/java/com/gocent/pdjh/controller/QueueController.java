package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.dto.PharmacyCheckInResult;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.service.LabCheckInService;
import com.gocent.pdjh.service.PharmacyCheckInService;
import com.gocent.pdjh.service.QueueService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final QueueService queueService;
    private final DepartmentRepository departmentRepository;
    private final TerminalBindingService terminalBindingService;
    private final PharmacyCheckInService pharmacyCheckInService;
    private final LabCheckInService labCheckInService;

    /** 医技类 businessType 集合（门诊医技 = 检验/超声/放射/心电/普通医技） */
    private static final Set<String> MED_TECH_TYPES = Set.of(
        "LAB", "EXAM", "RADIOLOGY", "ULTRASOUND", "ECG", "MEDTECH"
    );

    /**
     * 统一的全院(门诊/药房/检验)入队接口，供 HIS/第三方子系统调用
     * P1-2c: 若调用方未传 sourceType，后端根据科室 businessType 自动推断
     */
    @PostMapping("/enqueue")
    public ResultData<CallList> enqueue(@RequestBody EnqueueRequest request) {
        if (request.getDeptID() == null || request.getPatientName() == null) {
            return ResultData.fail(400, "参数缺失：deptID 和 patientName 不能为空");
        }
        if (request.getOfficeRoomId() == null || request.getOfficeRoomId().isBlank()) {
            return ResultData.fail(400, "参数缺失：officeRoomId（物理诊室）不能为空，普通号须指定诊室");
        }

        // P1-2c: 自动推断 sourceType（仅在调用方未显式传递时生效）
        if (request.getSourceType() == null || request.getSourceType().isEmpty()) {
            String inferred = inferSourceType(request.getDeptID());
            request.setSourceType(inferred);
            log.info("[P1-2c] 自动推断 sourceType={} (deptID={})", inferred, request.getDeptID());
        }
        
        try {
            CallList result = queueService.enqueue(request);
            return ResultData.success(result, "入队成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        } catch (Exception e) {
            return ResultData.fail(500, "入队失败：" + e.getMessage());
        }
    }

    /**
     * 根据科室 ID 推断来源类型
     *   - 医技科室 (LAB/EXAM/RADIOLOGY/ULTRASOUND/ECG) → OUTPATIENT_EXAM
     *   - 药房 (PHARMACY)                              → PHARMACY
     *   - 其他 / 科室不存在                            → CLINIC
     */
    private String inferSourceType(String deptId) {
        if (deptId == null || deptId.isEmpty()) return "CLINIC";
        try {
            return departmentRepository.findById(java.util.Objects.requireNonNull(deptId))
                .map(dept -> {
                    String bt = dept.getBusinessType();
                    if (bt == null) return "CLINIC";
                    if ("PHARMACY".equalsIgnoreCase(bt)) return "PHARMACY";
                    if (MED_TECH_TYPES.contains(bt.toUpperCase())) return "OUTPATIENT_EXAM";
                    return "CLINIC";
                })
                .orElse("CLINIC");
        } catch (Exception e) {
            log.warn("[P1-2c] 推断 sourceType 时查询科室失败，降级为 CLINIC: {}", e.getMessage());
            return "CLINIC";
        }
    }

    /**
     * 提供给第三方或 H5 使用的手动签到接口
     */
    @PostMapping("/signIn")
    public ResultData<Boolean> signIn(@RequestBody java.util.Map<String, String> request) {
        String patientCode = request.get("patientCode");
        if (patientCode == null || patientCode.isEmpty()) {
            return ResultData.fail(400, "参数缺失：patientCode 不能为空");
        }
        
        boolean ok = queueService.signIn(patientCode);
        if (ok) {
            return ResultData.success(true, "签到成功");
        } else {
            return ResultData.fail(404, "签到失败：未找到有效待签到记录");
        }
    }

    /**
     * 提供给 H5 获取签到前信息的接口
     */
    @PostMapping("/getSignInInfo")
    public ResultData<CallList> getSignInInfo(@RequestBody java.util.Map<String, String> request) {
        String patientCode = request.get("patientCode");
        String deviceCode = request.get("deviceCode");
        if (deviceCode == null || deviceCode.isEmpty()) {
            deviceCode = request.get("deviceId");
        }
        if (patientCode == null || patientCode.isEmpty()) {
            return ResultData.fail(400, "参数缺失：patientCode 不能为空");
        }
        if (deviceCode == null || deviceCode.isEmpty()) {
            return ResultData.fail(400, "参数缺失：deviceCode 不能为空");
        }

        java.util.List<String> allowedRoomIds = terminalBindingService.resolveSignDeviceRoomIds(deviceCode);
        CallList callList = terminalBindingService.matchPendingSignIn(patientCode, allowedRoomIds);
        if (callList != null) {
            return ResultData.success(callList, "获取待签到信息成功");
        }
        return ResultData.fail(404, "未找到有效待签到记录");
    }

    /**
     * 药房辅助签到：扫码入队 + 分窗 + 通知配药（PREPARING）。
     * 与门诊 getSignInInfo/CheckIn 不同：门诊先有 UNARRIVED 单，药房扫码才建单。
     */
    @PostMapping("/pharmacyCheckIn")
    public ResultData<PharmacyCheckInResult> pharmacyCheckIn(@RequestBody Map<String, String> request) {
        String patientCode = request.get("patientCode");
        String deviceCode = request.get("deviceCode");
        if (deviceCode == null || deviceCode.isEmpty()) {
            deviceCode = request.get("deviceId");
        }
        try {
            PharmacyCheckInResult result = pharmacyCheckInService.checkIn(patientCode, deviceCode);
            return ResultData.success(result, result.getMessage());
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("药房签到失败", e);
            return ResultData.fail(500, "药房签到失败：" + e.getMessage());
        }
    }

    /**
     * 药房配药完成：PREPARING → WAITING，供 HIS Webhook 或叫号器药房模式兜底调用
     */
    @PostMapping("/pharmacyMarkReady")
    public ResultData<CallList> pharmacyMarkReady(@RequestBody Map<String, Object> request) {
        Object idObj = request.get("callListId");
        if (idObj == null) {
            return ResultData.fail(400, "参数缺失：callListId");
        }
        Integer callListId;
        try {
            callListId = Integer.valueOf(idObj.toString());
        } catch (NumberFormatException e) {
            return ResultData.fail(400, "callListId 无效");
        }
        try {
            CallList updated = pharmacyCheckInService.markReady(callListId);
            return ResultData.success(updated, "已进入可取药队列");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 检验采血签到：扫码入队 + 分窗，直接 WAITING 可叫号。
     * 与 pharmacyCheckIn 不同：无 PREPARING；与 getSignInInfo 不同：无 HIS 单也可建单。
     */
    @PostMapping("/labCheckIn")
    public ResultData<PharmacyCheckInResult> labCheckIn(@RequestBody Map<String, String> request) {
        String patientCode = request.get("patientCode");
        String deviceCode = request.get("deviceCode");
        if (deviceCode == null || deviceCode.isEmpty()) {
            deviceCode = request.get("deviceId");
        }
        try {
            PharmacyCheckInResult result = labCheckInService.checkIn(patientCode, deviceCode);
            return ResultData.success(result, result.getMessage());
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("检验签到失败", e);
            return ResultData.fail(500, "检验签到失败：" + e.getMessage());
        }
    }

    /**
     * 叫号器药房窗口队列：配药中 + 请取药（按窗口）
     */
    @PostMapping("/pharmacyDeskQueue")
    public ResultData<List<CallList>> pharmacyDeskQueue(@RequestBody Map<String, String> request) {
        String officeRoomId = request.get("officeRoomId");
        if (officeRoomId == null || officeRoomId.isBlank()) {
            return ResultData.fail(400, "参数缺失：officeRoomId");
        }
        return ResultData.success(pharmacyCheckInService.listDeskQueue(officeRoomId), "查询成功");
    }
}
