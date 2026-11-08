package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.service.PatientFlowLogService;
import com.gocent.pdjh.service.QueueService;
import com.gocent.pdjh.enums.PatientFlowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final CallListRepository callListRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PatientFlowLogService patientFlowLogService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallList enqueue(EnqueueRequest request) {
        log.info("排队推单请求: deptID={}, patientName={}", request.getDeptID(), request.getPatientName());
        return processEnqueueSync(request);
    }

    private CallList processEnqueueSync(EnqueueRequest request) {
        CallList callList = new CallList();
        callList.setPatientCode(request.getPatientCode());
        callList.setPatientName(request.getPatientName());
        callList.setSex(request.getSex());
        callList.setAgeStr(request.getAgeStr());
        callList.setDeptID(request.getDeptID());
        callList.setSflbmc(request.getQueueType());
        callList.setShowNo(request.getShowNo());
        callList.setXh(request.getPriority() != null ? request.getPriority() : 0);
        callList.setDoctors(request.getDoctors());
        callList.setDoctorID(request.getDoctorID());

        callList.setIsAppointment(request.getIsAppointment() != null ? request.getIsAppointment() : 0);
        callList.setAppointmentStartTime(request.getAppointmentStartTime());
        callList.setAppointmentEndTime(request.getAppointmentEndTime());
        
        // P1-2c: 写入患者来源，调用方不传时兜底为普通门诊
        callList.setSourceType(request.getSourceType() != null ? request.getSourceType() : "CLINIC");
        if (request.getSflbbigtype() != null && !request.getSflbbigtype().isBlank()) {
            callList.setSflbbigtype(request.getSflbbigtype());
        }

        // 老代码中的 state=7 (签到等待) 已被废弃，现全局写入主池并在分诊引擎级拦截
        callList.setState(0);
        callList.setIsForzen(0);

        boolean directWaiting = Boolean.FALSE.equals(request.getRequireSignIn());
        if (directWaiting) {
            callList.setTriageState("WAITING");
            callList.setCheckInTime(new Date());
        } else {
            callList.setTriageState("UNARRIVED");
            callList.setCheckInTime(null);
        }
        callList.setCallCount(0);
        
        callList.setCallDate(new Date()); 
        callList.setCallTime(new Date());

        applyOfficeRoom(callList, request.getOfficeRoomId());
        if (callList.getOfficeRoomId() == null || callList.getOfficeRoomId().isBlank()) {
            throw new IllegalArgumentException("officeRoomId 无效：物理诊室不存在");
        }
        if (request.getDeptID() != null && callList.getDeptID() != null
                && !request.getDeptID().equals(callList.getDeptID())) {
            // 以诊室所属科室为准纠偏
            log.info("入队科室与诊室不一致，以诊室科室为准: req={}, roomDept={}", request.getDeptID(), callList.getDeptID());
        }

        CallList saved = callListRepository.save(callList);
        log.info("入队消费成功, 数据落盘ID: {}, sourceType: {}", saved.getId(), saved.getSourceType());
        // 提交后再广播：医生站/门边屏靠 STATE_UPDATE 即时拉取；大屏有轮询可兜底
        broadcastStateUpdateAfterCommit(saved.getId());
        return saved;
    }

    @Override
    public boolean signIn(String patientCode) {
        CallList callList = callListRepository.findFirstByPatientCodeAndTriageStateOrderByIdDesc(patientCode, "UNARRIVED");
        if (callList == null) {
            // 兼容老流水
            callList = callListRepository.findFirstByPatientCodeAndStateOrderByIdDesc(patientCode, 7);
        }
        if (callList == null) {
            log.warn("签到失败: 找不到患者为 {} 且处于未签到就绪池的记录", patientCode);
            return false;
        }

        callList.setState(0);
        callList.setTriageState("WAITING");
        callList.setCallTime(new Date());
        
        if (Integer.valueOf(1).equals(callList.getIsAppointment()) && callList.getAppointmentEndTime() != null) {
            try {
                java.time.LocalTime endTime = java.time.LocalTime.parse(callList.getAppointmentEndTime());
                java.time.LocalTime now = java.time.LocalTime.now();
                if (now.isAfter(endTime.plusMinutes(30))) {
                    log.info("患者 {} 预约时段 {} 超期30分钟，安全降级为现场普通号", patientCode, callList.getAppointmentEndTime());
                    callList.setIsAppointment(0);
                }
            } catch (Exception e) {}
        }
        
        callListRepository.save(callList);
        log.info("签到成功: 患者 {}, 已进入等待叫号池", patientCode);

        patientFlowLogService.record(callList, PatientFlowEvent.SIGN_IN, "TERMINAL",
                null, "签到终端", null, Map.of("patientCode", patientCode));
        
        eventPublisher.publishEvent(new com.gocent.pdjh.event.PatientSignInEvent(this, patientCode));
        broadcastStateUpdateAfterCommit(callList.getId());
        return true;
    }

    /**
     * 事务提交后推送 /topic/hall，避免医生站立刻刷新时读到未提交数据。
     * 与 NurseDesk / DoctorCall 的 STATE_UPDATE 口径一致。
     */
    private void broadcastStateUpdateAfterCommit(Integer callListId) {
        Runnable send = () -> {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "STATE_UPDATE");
            if (callListId != null) {
                msg.put("callListId", callListId);
            }
            messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
            log.debug("已广播 STATE_UPDATE callListId={}", callListId);
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

    @Override
    public CallList getPendingSignIn(String patientCode) {
        return getPendingSignIn(patientCode, null);
    }

    public CallList getPendingSignIn(String patientCode, List<String> allowedRoomIds) {
        if (allowedRoomIds != null && !allowedRoomIds.isEmpty()) {
            List<CallList> unarrived = callListRepository
                    .findByPatientCodeAndTriageStateAndOfficeRoomIdInOrderByIdDesc(patientCode, "UNARRIVED", allowedRoomIds);
            if (!unarrived.isEmpty()) {
                return unarrived.get(0);
            }
            CallList latestToday = callListRepository.findTodayLatestByPatientCode(patientCode);
            if (latestToday != null && latestToday.getOfficeRoomId() != null
                    && allowedRoomIds.contains(latestToday.getOfficeRoomId())) {
                return latestToday;
            }
            return null;
        }
        CallList unarrived = callListRepository.findFirstByPatientCodeAndTriageStateOrderByIdDesc(patientCode, "UNARRIVED");
        if (unarrived != null) {
            return unarrived;
        }
        return callListRepository.findTodayLatestByPatientCode(patientCode);
    }

    private void applyOfficeRoom(CallList callList, String officeRoomId) {
        if (officeRoomId == null || officeRoomId.isBlank()) {
            return;
        }
        String rid = officeRoomId.trim();
        OfficeRoom room = officeRoomRepository.findById(rid)
                .orElseThrow(() -> new IllegalArgumentException("物理诊室不存在：" + rid));
        callList.setOfficeRoomId(rid);
        callList.setOutPatientOffice(room.getName());
        if (callList.getDeptID() == null || callList.getDeptID().isBlank()) {
            callList.setDeptID(room.getDeptId());
        }
    }
}
