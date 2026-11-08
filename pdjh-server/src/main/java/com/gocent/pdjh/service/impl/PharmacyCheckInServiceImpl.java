package com.gocent.pdjh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.constants.TriageStateConstants;
import com.gocent.pdjh.dto.PharmacyCheckInResult;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.integration.HospitalIntegrationService;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.service.PatientFlowLogService;
import com.gocent.pdjh.service.PharmacyCheckInService;
import com.gocent.pdjh.service.QueueManager;
import com.gocent.pdjh.service.SysConfigService;
import com.gocent.pdjh.service.TerminalBindingService;
import com.gocent.pdjh.service.support.SignInQueueSupport;
import com.gocent.pdjh.service.support.SignInQueueSupport.SignInQueueFacts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 药房辅助签到实现：设备绑定药房诊室 → HIS/本地校验可入队 → 分窗 → PREPARING → 通知配药
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyCheckInServiceImpl implements PharmacyCheckInService {

    private final TerminalBindingService terminalBindingService;
    private final OfficeRoomRepository officeRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final CallListRepository callListRepository;
    private final QueueManager queueManager;
    private final SysConfigService sysConfigService;
    private final ObjectProvider<HospitalIntegrationService> hospitalIntegration;
    private final PatientFlowLogService patientFlowLogService;
    private final SignInQueueSupport signInQueueSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PharmacyCheckInResult checkIn(String patientCode, String deviceCode) {
        if (patientCode == null || patientCode.isBlank()) {
            throw new IllegalArgumentException("患者编号不能为空");
        }
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        String code = patientCode.trim();

        List<String> roomIds = terminalBindingService.resolveSignDeviceRoomIds(deviceCode);
        if (roomIds == null || roomIds.isEmpty()) {
            throw new IllegalArgumentException("签到终端尚未绑定诊室，请在管理后台完成配置");
        }

        List<OfficeRoom> pharmacyRooms = new ArrayList<>();
        for (String rid : roomIds) {
            OfficeRoom room = officeRoomRepository.findById(rid).orElse(null);
            if (room == null) continue;
            if (isPharmacyRoom(room)) {
                pharmacyRooms.add(room);
            }
        }
        if (pharmacyRooms.isEmpty()) {
            throw new IllegalArgumentException("本机未绑定药房诊室，无法办理取药签到");
        }

        // 幂等：今日同患者在本机绑定窗口内已有 PREPARING/WAITING
        for (OfficeRoom room : pharmacyRooms) {
            List<CallList> desk = callListRepository.findPharmacyDeskQueueByOfficeRoom(room.getId(), 0);
            for (CallList existing : desk) {
                if (code.equals(existing.getPatientCode())) {
                    return finish(existing, room.getName(), true, "您已签到，请按当前序号等候配药或叫号",
                            deviceCode, PatientFlowEvent.PHARMACY_SIGN_IN, "取药签到终端");
                }
            }
        }

        HisRxInfo rx = resolveHisPrescription(code);
        if (rx == null) {
            throw new IllegalArgumentException("未找到可取药处方，请确认已缴费或前往对应药房");
        }

        OfficeRoom targetRoom = pickWindow(pharmacyRooms);
        String algorithm = readSplitAlgorithm();
        String parentDeptId = targetRoom.getDeptId();
        if (parentDeptId != null) {
            Optional<Department> deptOpt = departmentRepository.findById(parentDeptId);
            if (deptOpt.isPresent() && deptOpt.get().getFatherID() != null && !deptOpt.get().getFatherID().isBlank()) {
                // 若当前为子窗口科室，用父科室做分流；再映射回本机绑定房间
                String selectedDeptId = queueManager.selectPharmacyWindow(deptOpt.get().getFatherID(), algorithm);
                OfficeRoom byDept = pharmacyRooms.stream()
                        .filter(r -> selectedDeptId.equals(r.getDeptId()))
                        .findFirst()
                        .orElse(null);
                if (byDept != null) {
                    targetRoom = byDept;
                }
            } else if (deptOpt.isPresent()) {
                String selectedDeptId = queueManager.selectPharmacyWindow(parentDeptId, algorithm);
                OfficeRoom byDept = pharmacyRooms.stream()
                        .filter(r -> selectedDeptId.equals(r.getDeptId()))
                        .findFirst()
                        .orElse(null);
                if (byDept != null) {
                    targetRoom = byDept;
                }
            }
        }

        CallList callList = new CallList();
        callList.setPatientCode(code);
        callList.setPatientName(rx.patientName != null ? rx.patientName : ("患者" + code));
        callList.setDeptID(targetRoom.getDeptId());
        callList.setOfficeRoomId(targetRoom.getId());
        callList.setOutPatientOffice(targetRoom.getName());
        callList.setSourceType("PHARMACY");
        callList.setSflbmc("取药");
        callList.setState(0);
        callList.setIsForzen(0);
        callList.setTriageState(TriageStateConstants.PREPARING);
        callList.setCheckInTime(new Date());
        callList.setCallDate(new Date());
        callList.setCallTime(new Date());
        callList.setCallCount(0);
        callList.setIsAppointment(0);

        Integer maxXh = callListRepository.findMaxXhTodayByDept(targetRoom.getDeptId());
        int nextXh = (maxXh != null ? maxXh : 0) + 1;
        callList.setXh(nextXh);
        callList.setShowNo("Y" + String.format("%03d", nextXh));

        CallList saved = callListRepository.save(callList);
        log.info("【药房签到】患者 {} 入队 PREPARING，窗口 {} ({})", code, targetRoom.getName(), targetRoom.getId());
        notifyHisArrived(code, targetRoom.getId(), saved.getShowNo());
        return finish(saved, targetRoom.getName(), false, "药房正在配药，请留意叫号",
                deviceCode, PatientFlowEvent.PHARMACY_SIGN_IN, "取药签到终端");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallList markReady(Integer callListId) {
        if (callListId == null) {
            throw new IllegalArgumentException("排队单 ID 不能为空");
        }
        CallList callList = callListRepository.findById(callListId)
                .orElseThrow(() -> new IllegalArgumentException("排队单不存在"));
        if (!"PHARMACY".equalsIgnoreCase(callList.getSourceType())
                && !isPharmacyDept(callList.getDeptID())) {
            throw new IllegalArgumentException("非药房排队单，无法标记配药完成");
        }
        if (!TriageStateConstants.PREPARING.equals(callList.getTriageState())) {
            if (TriageStateConstants.WAITING.equals(callList.getTriageState())) {
                return callList;
            }
            throw new IllegalArgumentException("当前状态不是配药中，无法标记完成");
        }
        callList.setTriageState(TriageStateConstants.WAITING);
        callList.setCallTime(new Date());
        CallList saved = callListRepository.save(callList);
        log.info("【药房配药完成】callListId={} 进入 WAITING 可叫号池", callListId);
        patientFlowLogService.record(saved, PatientFlowEvent.PHARMACY_READY, "PHARMACY",
                null, "药房配药", null, Map.of("callListId", callListId));
        return saved;
    }

    @Override
    public List<CallList> listDeskQueue(String officeRoomId) {
        if (officeRoomId == null || officeRoomId.isBlank()) {
            return List.of();
        }
        return callListRepository.findPharmacyDeskQueueByOfficeRoom(officeRoomId.trim(), 0);
    }

    private boolean isPharmacyRoom(OfficeRoom room) {
        if (room.getBusinessType() != null && "PHARMACY".equalsIgnoreCase(room.getBusinessType())) {
            return true;
        }
        return isPharmacyDept(room.getDeptId());
    }

    private boolean isPharmacyDept(String deptId) {
        if (deptId == null || deptId.isBlank()) return false;
        return departmentRepository.findById(deptId)
                .map(d -> "PHARMACY".equalsIgnoreCase(d.getBusinessType()))
                .orElse(false);
    }

    /** 在绑定窗口中选负载最低的（PREPARING+WAITING） */
    private OfficeRoom pickWindow(List<OfficeRoom> pharmacyRooms) {
        OfficeRoom best = pharmacyRooms.get(0);
        int minLoad = Integer.MAX_VALUE;
        for (OfficeRoom room : pharmacyRooms) {
            int load = callListRepository.findPharmacyDeskQueueByOfficeRoom(room.getId(), 0).size();
            if (load < minLoad) {
                minLoad = load;
                best = room;
            }
        }
        return best;
    }

    private String readSplitAlgorithm() {
        SysConfig config = sysConfigService.getConfigByKey("RULE_PHARMACY");
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isEmpty()) {
            return "SHORTEST_QUEUE";
        }
        try {
            JsonNode root = objectMapper.readTree(config.getConfigValue());
            if (root.has("splitAlgorithm")) {
                return root.get("splitAlgorithm").asText("SHORTEST_QUEUE");
            }
        } catch (Exception e) {
            log.warn("解析 RULE_PHARMACY.splitAlgorithm 失败: {}", e.getMessage());
        }
        return "SHORTEST_QUEUE";
    }

    /**
     * HIS 可入队校验。无集成 Bean 时：非空卡号视为可入队（联调 Mock）。
     * 有 Bean 时：用 getPatientRegistrationInfo；返回 null 则不可入队。
     */
    private HisRxInfo resolveHisPrescription(String patientCode) {
        HospitalIntegrationService his = hospitalIntegration.getIfAvailable();
        if (his == null) {
            log.info("【药房签到】无 HIS 集成 Bean，Mock 允许入队 patientCode={}", patientCode);
            HisRxInfo mock = new HisRxInfo();
            mock.patientCode = patientCode;
            mock.patientName = "患者" + patientCode;
            return mock;
        }
        try {
            var info = his.getPatientRegistrationInfo(patientCode);
            if (info == null) {
                return null;
            }
            HisRxInfo rx = new HisRxInfo();
            rx.patientCode = patientCode;
            rx.patientName = info.getPatientName() != null ? info.getPatientName() : ("患者" + patientCode);
            return rx;
        } catch (Exception e) {
            log.warn("【药房签到】HIS 查询失败，拒绝入队: {}", e.getMessage());
            return null;
        }
    }

    private void notifyHisArrived(String patientCode, String windowId, String showNo) {
        HospitalIntegrationService his = hospitalIntegration.getIfAvailable();
        if (his == null) return;
        try {
            // 约定：state=10 表示药房签到/开始配药（院方适配时映射）
            his.syncQueueStateToHis(patientCode, 10);
            log.info("【药房签到】已通知 HIS 患者到场配药 patient={} window={} no={}", patientCode, windowId, showNo);
        } catch (Exception e) {
            log.warn("【药房签到】通知 HIS 失败（本地单已落盘）: {}", e.getMessage());
        }
    }

    /** 填上队列人数、排队位置，并按统一文案写签到日志（含重复签到） */
    private PharmacyCheckInResult finish(CallList call, String roomName, boolean already, String message,
                                         String deviceCode, PatientFlowEvent event, String operatorName) {
        PharmacyCheckInResult result = toResult(call, roomName, already, message);
        SignInQueueFacts facts = signInQueueSupport.locate(call);
        result.setQueueCount(facts.queueCount());
        result.setQueuePosition(facts.queuePosition());
        result.setCheckInTime(signInQueueSupport.formatClock(call.getCheckInTime()));
        signInQueueSupport.recordTerminal(call, event, deviceCode, operatorName, already, facts);
        return result;
    }

    private PharmacyCheckInResult toResult(CallList c, String roomName, boolean already, String message) {
        PharmacyCheckInResult r = new PharmacyCheckInResult();
        r.setCallListId(c.getId());
        r.setPatientCode(c.getPatientCode());
        r.setPatientName(c.getPatientName());
        r.setShowNo(c.getShowNo());
        r.setDeptId(c.getDeptID());
        r.setOfficeRoomId(c.getOfficeRoomId());
        r.setOfficeRoomName(roomName != null ? roomName : c.getOutPatientOffice());
        r.setTriageState(c.getTriageState());
        r.setAlreadyCheckedIn(already);
        r.setMessage(message);
        return r;
    }

    private static final class HisRxInfo {
        String patientCode;
        String patientName;
    }
}
