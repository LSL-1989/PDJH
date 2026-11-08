package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constants.TriageStateConstants;
import com.gocent.pdjh.dto.PharmacyCheckInResult;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.integration.HospitalIntegrationService;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.service.LabCheckInService;
import com.gocent.pdjh.service.TerminalBindingService;
import com.gocent.pdjh.service.TriageService;
import com.gocent.pdjh.service.support.SignInQueueSupport;
import com.gocent.pdjh.service.support.SignInQueueSupport.SignInQueueFacts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检验采血签到：设备绑定检验窗口 → 已有 UNARRIVED 则核销，否则扫码建单 → 最短队列分窗 → WAITING
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabCheckInServiceImpl implements LabCheckInService {

    private final TerminalBindingService terminalBindingService;
    private final OfficeRoomRepository officeRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final CallListRepository callListRepository;
    private final TriageService triageService;
    private final ObjectProvider<HospitalIntegrationService> hospitalIntegration;
    private final SignInQueueSupport signInQueueSupport;
    private final SimpMessagingTemplate messagingTemplate;

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

        List<OfficeRoom> labRooms = new ArrayList<>();
        List<String> labRoomIds = new ArrayList<>();
        for (String rid : roomIds) {
            OfficeRoom room = officeRoomRepository.findById(rid).orElse(null);
            if (room == null) continue;
            if (isLabRoom(room)) {
                labRooms.add(room);
                labRoomIds.add(room.getId());
            }
        }
        if (labRooms.isEmpty()) {
            throw new IllegalArgumentException("本机未绑定检验诊室，无法办理采血签到");
        }

        List<CallList> todayInLab = callListRepository.findTodayByPatientCodeAndOfficeRoomIdIn(code, labRoomIds);
        if (!todayInLab.isEmpty()) {
            CallList existing = todayInLab.get(0);
            OfficeRoom existingRoom = officeRoomRepository.findById(
                    existing.getOfficeRoomId() != null ? existing.getOfficeRoomId() : "").orElse(null);
            String roomName = existingRoom != null ? existingRoom.getName() : existing.getOutPatientOffice();

            if (TriageStateConstants.UNARRIVED.equalsIgnoreCase(existing.getTriageState())) {
                boolean ok = triageService.checkIn(existing.getId(), deviceCode);
                if (!ok) {
                    throw new IllegalArgumentException("签到失败：待检记录状态异常，请联系护士台");
                }
                CallList signed = callListRepository.findById(existing.getId()).orElse(existing);
                broadcastStateUpdateAfterCommit(signed.getId());
                return finish(signed, roomName, false, "请到【" + roomName + "】等候采血叫号", deviceCode);
            }

            Integer st = existing.getState();
            if (Integer.valueOf(4).equals(st)) {
                throw new IllegalArgumentException("您今日已完成采血，如需复查请到护士台办理");
            }
            if (Integer.valueOf(6).equals(st)) {
                throw new IllegalArgumentException("该号已过号，请到护士台处理后再签到");
            }
            return finish(existing, roomName, true, "您已签到，请按当前序号等候采血叫号", deviceCode);
        }

        HisLabInfo info = resolveHisOrder(code);
        if (info == null) {
            throw new IllegalArgumentException("未找到可检验项目，请确认已缴费或前往对应窗口");
        }

        OfficeRoom targetRoom = pickWindow(labRooms);

        CallList callList = new CallList();
        callList.setPatientCode(code);
        callList.setPatientName(info.patientName != null ? info.patientName : ("患者" + code));
        callList.setDeptID(targetRoom.getDeptId());
        callList.setOfficeRoomId(targetRoom.getId());
        callList.setOutPatientOffice(targetRoom.getName());
        callList.setSourceType("OUTPATIENT_EXAM");
        callList.setSflbmc("检验");
        callList.setState(0);
        callList.setIsForzen(0);
        callList.setTriageState(TriageStateConstants.WAITING);
        callList.setCheckInTime(new Date());
        callList.setCallDate(new Date());
        callList.setCallTime(new Date());
        callList.setCallCount(0);
        callList.setIsAppointment(0);

        Integer maxXh = callListRepository.findMaxXhTodayByDept(targetRoom.getDeptId());
        int nextXh = (maxXh != null ? maxXh : 0) + 1;
        callList.setXh(nextXh);
        callList.setShowNo("J" + String.format("%03d", nextXh));

        CallList saved = callListRepository.save(callList);
        log.info("【检验签到】患者 {} 入队 WAITING，窗口 {} ({})", code, targetRoom.getName(), targetRoom.getId());

        broadcastStateUpdateAfterCommit(saved.getId());
        return finish(saved, targetRoom.getName(), false,
                "请到【" + targetRoom.getName() + "】等候采血叫号", deviceCode);
    }

    private boolean isLabRoom(OfficeRoom room) {
        if (room.getBusinessType() != null && "LAB".equalsIgnoreCase(room.getBusinessType())) {
            return true;
        }
        return isLabDept(room.getDeptId());
    }

    private boolean isLabDept(String deptId) {
        if (deptId == null || deptId.isBlank()) return false;
        return departmentRepository.findById(deptId)
                .map(d -> "LAB".equalsIgnoreCase(d.getBusinessType()))
                .orElse(false);
    }

    /** 绑定窗口中选等候+正在采血人数最少的 */
    private OfficeRoom pickWindow(List<OfficeRoom> labRooms) {
        OfficeRoom best = labRooms.get(0);
        int minLoad = Integer.MAX_VALUE;
        for (OfficeRoom room : labRooms) {
            int waiting = callListRepository.findTodayCallListByOfficeRoomAndStateForDoctor(room.getId(), 0).size();
            int calling = callListRepository.findTodayCallListByOfficeRoomAndStateForDoctor(room.getId(), 1).size();
            int load = waiting + calling;
            if (load < minLoad) {
                minLoad = load;
                best = room;
            }
        }
        return best;
    }

    /**
     * HIS 可入队校验。无集成 Bean 时：非空卡号视为可入队（联调 Mock）。
     */
    private HisLabInfo resolveHisOrder(String patientCode) {
        HospitalIntegrationService his = hospitalIntegration.getIfAvailable();
        if (his == null) {
            log.info("【检验签到】无 HIS 集成 Bean，Mock 允许入队 patientCode={}", patientCode);
            HisLabInfo mock = new HisLabInfo();
            mock.patientCode = patientCode;
            mock.patientName = "患者" + patientCode;
            return mock;
        }
        try {
            var info = his.getPatientRegistrationInfo(patientCode);
            if (info == null) {
                return null;
            }
            HisLabInfo lab = new HisLabInfo();
            lab.patientCode = patientCode;
            lab.patientName = info.getPatientName() != null ? info.getPatientName() : ("患者" + patientCode);
            return lab;
        } catch (Exception e) {
            log.warn("【检验签到】HIS 查询失败，拒绝入队: {}", e.getMessage());
            return null;
        }
    }

    private void broadcastStateUpdateAfterCommit(Integer callListId) {
        Runnable send = () -> {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "STATE_UPDATE");
            if (callListId != null) {
                msg.put("callListId", callListId);
            }
            messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    /** 填上队列人数、排队位置，并按统一文案写签到日志（含重复签到） */
    private PharmacyCheckInResult finish(CallList call, String roomName, boolean already, String message, String deviceCode) {
        PharmacyCheckInResult result = toResult(call, roomName, already, message);
        SignInQueueFacts facts = signInQueueSupport.locate(call);
        result.setQueueCount(facts.queueCount());
        result.setQueuePosition(facts.queuePosition());
        result.setCheckInTime(signInQueueSupport.formatClock(call.getCheckInTime()));
        signInQueueSupport.recordTerminal(call, PatientFlowEvent.LAB_SIGN_IN, deviceCode, "检验签到终端", already, facts);
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

    private static final class HisLabInfo {
        String patientCode;
        String patientName;
    }
}
