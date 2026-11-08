package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.DocCallMsgInput;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.service.DoctorCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import com.gocent.pdjh.utils.DesensitizeUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.service.SysConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorCallServiceImpl implements DoctorCallService {

    private final CallListRepository callListRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.gocent.pdjh.repository.DepartmentRepository departmentRepository;
    private final com.gocent.pdjh.repository.OfficeRoomRepository officeRoomRepository;
    private final DoctorRepository doctorRepository;
    private final RedissonClient redissonClient;
    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final com.gocent.pdjh.integration.IntegrationEventPublisher integrationEventPublisher;
    private final com.gocent.pdjh.service.SystemActionLogService systemActionLogService;
    private final com.gocent.pdjh.service.PatientFlowLogService patientFlowLogService;
    private final com.gocent.pdjh.service.VoiceDeviceService voiceDeviceService;
    private final com.gocent.pdjh.repository.ExamRecordDetailRepository examRecordDetailRepository;
    private final com.gocent.pdjh.repository.DictExamItemRepository dictExamItemRepository;
    private final com.gocent.pdjh.service.ExamRoutingService examRoutingService;

    @Override
    public List<CallList> getCallList(String deptId, Integer state, String officeRoomId) {
        List<CallList> list;
        if (officeRoomId != null && !officeRoomId.isBlank()) {
            log.info("查询诊室 officeRoomId={} state={} 排队情况", officeRoomId, state);
            list = callListRepository.findTodayCallListByOfficeRoomAndStateForDoctor(officeRoomId.trim(), state);
        } else {
            log.info("查询科室 {} ，状态 {} 的排队情况", deptId, state);
            list = callListRepository.findTodayCallListByDeptAndState(deptId, state);
        }
        attachExamHints(list);
        return list;
    }

    @Override
    public Map<String, Object> getPaginatedCallList(com.gocent.pdjh.dto.QueryInput input) {
        return paginatedCallList(input, null);
    }

    @Override
    public Map<String, Object> getDeptCallList(com.gocent.pdjh.dto.QueryInput input) {
        List<String> scope = expandDeptScope(resolveDeptIdToken(input.getDeptID()));
        return paginatedCallList(input, scope);
    }

    private List<String> expandDeptScope(String seedDeptId) {
        if (seedDeptId == null || seedDeptId.isEmpty()) {
            return List.of();
        }
        Optional<Department> dOpt = departmentRepository.findById(seedDeptId);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (dOpt.isEmpty()) {
            ids.add(seedDeptId);
            return new ArrayList<>(ids);
        }
        Department d = dOpt.get();
        String parentKey = (d.getFatherID() != null && !d.getFatherID().isEmpty()
                && !"00000000-0000-0000-0000-000000000000".equalsIgnoreCase(d.getFatherID()))
                ? d.getFatherID()
                : d.getId();
        ids.add(parentKey);
        departmentRepository.findByFatherID(parentKey).forEach(ch -> ids.add(ch.getId()));
        return new ArrayList<>(ids);
    }

    private String resolveDeptIdToken(Object deptField) {
        if (deptField == null) {
            return "";
        }
        if (deptField instanceof List<?> list && !list.isEmpty()) {
            return list.get(list.size() - 1).toString();
        }
        return deptField.toString();
    }

    private Map<String, Object> paginatedCallList(com.gocent.pdjh.dto.QueryInput input, List<String> deptScopeOverride) {
        Specification<CallList> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (input.getPatientCode() != null && !input.getPatientCode().isEmpty()) {
                predicates.add(cb.equal(root.get("patientCode"), input.getPatientCode()));
            }
            if (input.getPatientName() != null && !input.getPatientName().isEmpty()) {
                predicates.add(cb.like(root.get("patientName"), "%" + input.getPatientName() + "%"));
            }
            if (input.getCallTime() != null && !input.getCallTime().isEmpty()) {
                predicates.add(cb.like(root.get("callTime").as(String.class), input.getCallTime() + "%"));
            }
            if (input.getDoctors() != null && !input.getDoctors().isEmpty()) {
                predicates.add(cb.equal(root.get("doctors"), input.getDoctors()));
            }

            List<String> targetDeptIds = new ArrayList<>();
            if (deptScopeOverride != null && !deptScopeOverride.isEmpty()) {
                targetDeptIds.addAll(deptScopeOverride);
            } else if (input.getDeptID() != null) {
                if (input.getDeptID() instanceof List) {
                    List<?> list = (List<?>) input.getDeptID();
                    if (!list.isEmpty()) {
                        targetDeptIds.add(list.get(list.size() - 1).toString());
                    }
                } else if (!input.getDeptID().toString().isEmpty()) {
                    targetDeptIds.add(input.getDeptID().toString());
                }
            }

            if (input.getBusinessType() != null && !input.getBusinessType().isEmpty()) {
                List<Department> allowedDepts = departmentRepository.findAll();
                List<String> bTypeDepts = allowedDepts.stream()
                        .filter(d -> input.getBusinessType().equals(d.getBusinessType()))
                        .map(Department::getId)
                        .toList();

                if (targetDeptIds.isEmpty()) {
                    targetDeptIds.addAll(bTypeDepts);
                } else {
                    targetDeptIds.retainAll(bTypeDepts);
                }

                if (targetDeptIds.isEmpty()) {
                    predicates.add(cb.equal(root.get("id"), -1));
                }
            }

            if (!targetDeptIds.isEmpty()) {
                predicates.add(root.get("deptID").in(targetDeptIds));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int pageIdx = (input.getPageIndex() != null && input.getPageIndex() > 0) ? input.getPageIndex() - 1 : 0;
        int pageSize = (input.getPageSize() != null && input.getPageSize() > 0) ? input.getPageSize() : 10;
        PageRequest pageRequest = PageRequest.of(pageIdx, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<CallList> page = callListRepository.findAll(spec, pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("TotalCount", page.getTotalElements());
        result.put("data", page.getContent());
        result.put("pageInfo", Map.of("totalCount", page.getTotalElements()));
        return result;
    }

    @Override
    public boolean editCallMsgState(Integer callListId, Integer state, String officeRoomId, String doctorId) {
        RLock lock = redissonClient.getLock("pdjh:lock:callList:" + callListId);
        try {
            if (!lock.tryLock(2, TimeUnit.SECONDS)) {
                log.warn("获取分布式锁失败，存在并发修改，ID: {}", callListId);
                return false;
            }
            try {
                Optional<CallList> record = callListRepository.findById(callListId);
                if (record.isPresent()) {
                    CallList callList = record.get();
                    callList.setState(state);
                    
                    if (state == 1 && callList.getStartTime() == null) {
                        callList.setStartTime(new java.util.Date());
                    }
                    if (state == 1) {
                        bindRoomAndDoctor(callList, officeRoomId, doctorId);
                    }
                    if (state == 4) {
                        callList.setEndDate(new java.util.Date());
                    }
                    
                    callListRepository.save(callList);
                    log.info("更新患者 ID {} 状态为 {}", callListId, state);
                    
                    messagingTemplate.convertAndSend("/topic/hall", (Object) Map.of(
                            "type", "STATE_UPDATE",
                            "callListId", callListId,
                            "state", state
                    ));
                    integrationEventPublisher.publishEvent("STATE_UPDATE", Map.of(
                            "callListId", callListId,
                            "state", state
                    ));
                    
                    // P5: 记录操作审计日志
                    systemActionLogService.recordLogAsync(
                            "DOCTOR_CALL",
                            "Doctor_Operation",
                            "修改排号状态为: " + state + " (ID:" + callListId + ")",
                            Map.of("callListId", callListId, "newState", state),
                            true,
                            null
                    );
                    recordPatientFlowForState(callList, state, doctorId);

                    if (state == 4) {
                        examRoutingService.completeFinishedCall(callList);
                    }
                    return true;
                }
                return false;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式锁获取被中断", e);
            return false;
        }
    }

    @Override
    public boolean docCallMsg(DocCallMsgInput input) {
        if (input == null || input.getMsg() == null || input.getMsg().trim().isEmpty()) {
            return false;
        }

        String[] parts = input.getMsg().split("\\|");
        if (parts.length < 1) return false;

        try {
            int callListId;
            try {
                callListId = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                log.error("前端呼叫指令解析整型 ID 失败，恶意数据或传参丢失: {}", parts[0]);
                return false;
            }
            
            RLock lock = redissonClient.getLock("pdjh:lock:callList:" + callListId);
            try {
                if (!lock.tryLock(2, TimeUnit.SECONDS)) {
                    log.warn("呼叫时获取分布式锁失败，防止并发重复呼叫，ID: {}", callListId);
                    return false;
                }
                try {
                    Optional<CallList> patientOpt = callListRepository.findById(callListId);
                    if (patientOpt.isEmpty()) {
                        log.warn("未能找到指定的排队记录，ID: {}", callListId);
                        return false;
                    }

                    CallList patient = patientOpt.get();
                    String patientName = patient.getPatientName();
                    String office = parts.length > 1 ? parts[1] : patient.getOutPatientOffice();
                    String showNo = patient.getShowNo();
                    String callCode = input.getCode() != null ? input.getCode() : "";

                    // 播报文案与候诊大屏「语音播报」CallContent 一致：{0}号序 {1}姓名 {2}诊室
                    String callText = voiceDeviceService.formatCallText(
                            patient.getOfficeRoomId(), callCode, showNo, patientName, office);

                    String voiceChannel = voiceDeviceService.resolveVoiceChannel(
                            patient.getOfficeRoomId(), callCode);
                    String speakScreenId = voiceDeviceService.resolveSpeakScreenId(
                            patient.getOfficeRoomId(), callCode);
                    Map<String, Object> speakOpts = voiceDeviceService.resolveSpeakOpts(
                            patient.getOfficeRoomId(), callCode);
                    boolean pushVoice = VoiceDeviceServiceImpl.CHANNEL_VOICE.equals(voiceChannel)
                            || VoiceDeviceServiceImpl.CHANNEL_BOTH.equals(voiceChannel);
                    boolean speakOnScreen = VoiceDeviceServiceImpl.CHANNEL_SCREEN.equals(voiceChannel)
                            || VoiceDeviceServiceImpl.CHANNEL_BOTH.equals(voiceChannel);
                    
                    Map<String, Object> broadcastMsg = new java.util.HashMap<>();
                    broadcastMsg.put("type", "CALL_MSG");
                    broadcastMsg.put("code", callCode);
                    broadcastMsg.put("patientName", patientName);
                    broadcastMsg.put("showNo", showNo != null ? showNo : "");
                    broadcastMsg.put("office", office != null ? office : "");
                    broadcastMsg.put("officeRoomId", patient.getOfficeRoomId() != null ? patient.getOfficeRoomId() : "");
                    broadcastMsg.put("callText", callText);
                    broadcastMsg.put("voiceChannel", voiceChannel);
                    broadcastMsg.put("speakOnScreen", speakOnScreen);
                    broadcastMsg.put("callNum", speakOpts.get("callNum"));
                    broadcastMsg.put("rate", speakOpts.get("rate"));
                    if (speakScreenId != null) {
                        broadcastMsg.put("speakScreenId", speakScreenId);
                    }
                    log.info("向 WebSocket 发送局部大厅原名广播(含语音): {} channel={}", callText, voiceChannel);
                    messagingTemplate.convertAndSend("/topic/hall", (Object) broadcastMsg);
                    integrationEventPublisher.publishEvent("CALL_MSG", broadcastMsg);
                    
                    // 全院大屏走脱敏策略（同一模板，姓名脱敏）
                    String shieldName = DesensitizeUtil.desensitizeChineseName(patientName);
                    String shieldCallText = voiceDeviceService.formatCallText(
                            patient.getOfficeRoomId(), callCode, showNo, shieldName, office);
                    Map<String, Object> globalBroadcastMsg = new java.util.HashMap<>();
                    globalBroadcastMsg.put("type", "CALL_MSG");
                    globalBroadcastMsg.put("code", callCode);
                    globalBroadcastMsg.put("patientName", shieldName);
                    globalBroadcastMsg.put("ttsName", patientName); // 提供单独TTS名字给能够发声的端但屏幕显示屏蔽后的名字
                    globalBroadcastMsg.put("showNo", showNo != null ? showNo : "");
                    globalBroadcastMsg.put("office", office != null ? office : "");
                    globalBroadcastMsg.put("officeRoomId", patient.getOfficeRoomId() != null ? patient.getOfficeRoomId() : "");
                    globalBroadcastMsg.put("callText", shieldCallText);
                    globalBroadcastMsg.put("ttsCallText", callText); // 用于发声音的文本
                    globalBroadcastMsg.put("voiceChannel", voiceChannel);
                    globalBroadcastMsg.put("speakOnScreen", speakOnScreen);
                    globalBroadcastMsg.put("callNum", speakOpts.get("callNum"));
                    globalBroadcastMsg.put("rate", speakOpts.get("rate"));
                    if (speakScreenId != null) {
                        globalBroadcastMsg.put("speakScreenId", speakScreenId);
                    }
                    log.info("向 WebSocket 发送全院大屏脱敏广播: {}", shieldCallText);
                    messagingTemplate.convertAndSend("/topic/global-call", (Object) globalBroadcastMsg);

                    // 独立音响：由候诊大屏端绑定；SCREEN 通道不推音响
                    int voicePushed = 0;
                    if (pushVoice) {
                        voicePushed = voiceDeviceService.broadcastCall(
                                patient.getOfficeRoomId(),
                                showNo,
                                patientName,
                                office,
                                Map.of(
                                        "showNo", showNo != null ? showNo : "",
                                        "patientName", patientName != null ? patientName : "",
                                        "office", office != null ? office : "",
                                        "callListId", callListId
                                )
                        );
                        // VOICE 通道但无可用音响时自动降级为大屏 TTS
                        if (voicePushed == 0 && VoiceDeviceServiceImpl.CHANNEL_VOICE.equals(voiceChannel)) {
                            speakOnScreen = true;
                            broadcastMsg.put("speakOnScreen", true);
                            globalBroadcastMsg.put("speakOnScreen", true);
                            messagingTemplate.convertAndSend("/topic/hall", (Object) broadcastMsg);
                            messagingTemplate.convertAndSend("/topic/global-call", (Object) globalBroadcastMsg);
                            log.info("无可用独立音响，已降级为大屏 TTS speakScreenId={}", speakScreenId);
                        }
                    }
                    
                    // P5: 记录语音安全与播报审计日志
                    systemActionLogService.recordLogAsync(
                            "VOICE_BROADCAST",
                            office,
                            "播报[" + voiceChannel + "] voice=" + voicePushed + " screenTTS=" + speakOnScreen + ": " + callText,
                            globalBroadcastMsg,
                            true,
                            null
                    );
                    
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("尝试获取锁时被中断", e);
                return false;
            }
        } catch (Exception e) {
            log.error("处理叫号操作时发生异常", e);
            return false;
        }
    }

    @Override
    public boolean emergencyOverride(Integer callListId, String action) {
        RLock lock = redissonClient.getLock("pdjh:lock:callList:" + callListId);
        try {
            if (!lock.tryLock(2, TimeUnit.SECONDS)) {
                log.warn("紧急调度获取分布式锁失败，ID: {}", callListId);
                return false;
            }
            try {
                Optional<CallList> record = callListRepository.findById(callListId);
                if (record.isPresent()) {
                    CallList callList = record.get();
                    if ("BOOST".equalsIgnoreCase(action)) {
                        // 转为复诊池：穿插调度与医生站 Tab 均认 triageState=REVISIT
                        callList.setIsAppointment(9);
                        callList.setXh(0);
                        callList.setState(0);
                        callList.setTriageState("REVISIT");
                        log.info("患者 ID {} 转为复诊优先（triageState=REVISIT）", callListId);
                    } else if ("SUSPEND".equalsIgnoreCase(action)) {
                        callList.setIsAppointment(-1); 
                        callList.setXh(999999);
                        callList.setCallTime(new java.util.Date());
                        callList.setState(0);
                        log.info("患者 ID {} 被设置为挂起延后", callListId);
                    }
                    callListRepository.save(callList);
                    
                    messagingTemplate.convertAndSend("/topic/hall", (Object) Map.of(
                            "type", "STATE_UPDATE",
                            "callListId", callListId,
                            "action", action
                    ));
                    if ("BOOST".equalsIgnoreCase(action)) {
                        patientFlowLogService.record(callList, com.gocent.pdjh.enums.PatientFlowEvent.REVISIT,
                                "DOCTOR", doctorIdSafe(null), "医生站", "复诊优先", Map.of("action", "BOOST"));
                    }
                    return true;
                }
                return false;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式锁获取被中断", e);
            return false;
        }
    }

    /**
     * 接诊时写入诊室/医生，保证门边屏按 office_room 能刷到当前人。
     */
    private void bindRoomAndDoctor(CallList callList, String officeRoomId, String doctorId) {
        if (officeRoomId != null && !officeRoomId.isBlank()) {
            String roomId = officeRoomId.trim();
            callList.setOfficeRoomId(roomId);
            officeRoomRepository.findById(roomId).ifPresent(room -> {
                if (room.getName() != null && !room.getName().isBlank()) {
                    callList.setOutPatientOffice(room.getName());
                }
            });
        }
        if (doctorId != null && !doctorId.isBlank()) {
            callList.setDoctorID(doctorId.trim());
        }
    }

    /** 按状态写入患者流程留痕；药房科室走药房事件码 */
    private void recordPatientFlowForState(CallList callList, Integer state, String doctorId) {
        if (callList == null || state == null) {
            return;
        }
        boolean pharmacy = isPharmacyCall(callList);
        boolean lab = !pharmacy && isLabCall(callList);
        com.gocent.pdjh.enums.PatientFlowEvent event = null;
        if (state == 1) {
            event = pharmacy ? com.gocent.pdjh.enums.PatientFlowEvent.PHARMACY_CALL
                    : (lab ? com.gocent.pdjh.enums.PatientFlowEvent.LAB_CALL
                    : com.gocent.pdjh.enums.PatientFlowEvent.CALL);
        } else if (state == 4) {
            event = pharmacy ? com.gocent.pdjh.enums.PatientFlowEvent.PHARMACY_COMPLETE
                    : (lab ? com.gocent.pdjh.enums.PatientFlowEvent.LAB_COMPLETE
                    : com.gocent.pdjh.enums.PatientFlowEvent.COMPLETE);
        } else if (state == 6) {
            event = pharmacy ? com.gocent.pdjh.enums.PatientFlowEvent.PHARMACY_SKIP
                    : (lab ? com.gocent.pdjh.enums.PatientFlowEvent.LAB_SKIP
                    : com.gocent.pdjh.enums.PatientFlowEvent.SKIP);
        }
        if (event == null) {
            return;
        }
        String source = pharmacy ? "PHARMACY" : (lab ? "LAB" : "DOCTOR");
        patientFlowLogService.record(callList, event, source, doctorIdSafe(doctorId),
                pharmacy ? "药房窗口" : (lab ? "检验窗口" : "HJ叫号器"),
                com.gocent.pdjh.service.support.PatientFlowDisplayText.remarkForState(event.getCode(), state),
                Map.of("state", state, "officeRoomId",
                        callList.getOfficeRoomId() != null ? callList.getOfficeRoomId() : ""));
    }

    private boolean isPharmacyCall(CallList callList) {
        if (callList.getSourceType() != null && "PHARMACY".equalsIgnoreCase(callList.getSourceType())) {
            return true;
        }
        if (callList.getDeptID() == null || callList.getDeptID().isBlank()) {
            return false;
        }
        return departmentRepository.findById(callList.getDeptID())
                .map(d -> d.getBusinessType() != null && "PHARMACY".equalsIgnoreCase(d.getBusinessType()))
                .orElse(false);
    }

    private boolean isLabCall(CallList callList) {
        if (callList.getDeptID() == null || callList.getDeptID().isBlank()) {
            return false;
        }
        return departmentRepository.findById(callList.getDeptID())
                .map(d -> d.getBusinessType() != null && "LAB".equalsIgnoreCase(d.getBusinessType()))
                .orElse(false);
    }

    private static String doctorIdSafe(String doctorId) {
        return doctorId != null && !doctorId.isBlank() ? doctorId.trim() : null;
    }

    @Override
    public CallList autoCallNext(String deptId, String officeRoomId, String doctorId) {
        String roomKey = (officeRoomId != null && !officeRoomId.isBlank()) ? officeRoomId.trim() : "";
        String lockName = roomKey.isEmpty()
                ? "pdjh:lock:autoCall:" + deptId
                : "pdjh:lock:autoCall:" + deptId + ":" + roomKey;
        RLock deptLock = redissonClient.getLock(lockName);
        try {
            if (!deptLock.tryLock(3, TimeUnit.SECONDS)) {
                log.warn("获取呼叫自动调度锁失败: {}", lockName);
                return null;
            }
            try {
                String businessType = "OUTPATIENT";
                Optional<Department> deptOpt = departmentRepository.findById(deptId);
                if (deptOpt.isPresent() && deptOpt.get().getBusinessType() != null) {
                    businessType = deptOpt.get().getBusinessType();
                }

                CallList chosenOne = null;
                boolean roomScoped = !roomKey.isEmpty();

                if ("PHARMACY".equals(businessType)) {
                    String urgentStrategy = "EMERGENCY_FIRST";
                    SysConfig config = sysConfigService.getConfigByKey("RULE_PHARMACY");
                    if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                        try {
                            JsonNode root = objectMapper.readTree(config.getConfigValue());
                            if (root.has("urgentStrategy")) {
                                urgentStrategy = root.get("urgentStrategy").asText();
                            }
                        } catch (Exception e) {
                            log.error("【药房调度】解析 RULE_PHARMACY 配置失败: {}", e.getMessage());
                        }
                    }

                    List<CallList> pharmacyQueue;
                    boolean urgent = "EMERGENCY_FIRST".equals(urgentStrategy) || "VIP_FIRST".equals(urgentStrategy);
                    if (roomScoped) {
                        pharmacyQueue = urgent
                                ? callListRepository.findPharmacyQueueListUrgentFirstByOfficeRoom(roomKey, 0)
                                : callListRepository.findPharmacyQueueListByOfficeRoom(roomKey, 0);
                    } else {
                        pharmacyQueue = urgent
                                ? callListRepository.findPharmacyQueueListUrgentFirst(deptId, 0)
                                : callListRepository.findPharmacyQueueList(deptId, 0);
                    }
                    if (!pharmacyQueue.isEmpty()) {
                        chosenOne = pharmacyQueue.get(0);
                    }
                } else if ("LAB".equalsIgnoreCase(businessType)) {
                    List<CallList> labQueue = roomScoped
                            ? callListRepository.findTodayFirstVisitListByOfficeRoom(roomKey, 0)
                            : callListRepository.findTodayFirstVisitList(deptId, 0);
                    if (!labQueue.isEmpty()) {
                        chosenOne = labQueue.get(0);
                        log.info("【检验调度】窗口/科室 {} 选出 {}", roomScoped ? roomKey : deptId,
                                chosenOne.getPatientName());
                    }
                } else {
                    List<CallList> firstVisitQueue = roomScoped
                            ? callListRepository.findTodayFirstVisitListByOfficeRoom(roomKey, 0)
                            : callListRepository.findTodayFirstVisitList(deptId, 0);
                    List<CallList> returnVisitQueue = roomScoped
                            ? callListRepository.findTodayReturnVisitListByOfficeRoom(roomKey, 0)
                            : callListRepository.findTodayReturnVisitList(deptId, 0);

                    String counterKey = roomScoped
                            ? "pdjh:callCounter:" + deptId + ":" + roomKey
                            : "pdjh:callCounter:" + deptId;
                    org.redisson.api.RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
                    long currentCount = counter.get();
                    String scopeLabel = roomScoped ? roomKey : deptId;

                    if (!firstVisitQueue.isEmpty() && !returnVisitQueue.isEmpty()) {
                        if (currentCount < 3) {
                            chosenOne = firstVisitQueue.get(0);
                            counter.incrementAndGet();
                            log.info("【穿插调度: {}】当前计次{}, 抽取初诊: {}", scopeLabel, currentCount, chosenOne.getPatientName());
                        } else {
                            chosenOne = returnVisitQueue.get(0);
                            counter.set(0);
                            log.info("【穿插调度: {}】当前计次{}, 达到阈值，强制穿插复诊: {}", scopeLabel, currentCount, chosenOne.getPatientName());
                        }
                    } else if (!firstVisitQueue.isEmpty()) {
                        chosenOne = firstVisitQueue.get(0);
                        counter.incrementAndGet();
                        log.info("【单轨调度: {}】复诊池为空，抽取初诊: {}", scopeLabel, chosenOne.getPatientName());
                    } else if (!returnVisitQueue.isEmpty()) {
                        chosenOne = returnVisitQueue.get(0);
                        counter.set(0);
                        log.info("【单轨调度: {}】初诊池为空，抽取复诊: {}", scopeLabel, chosenOne.getPatientName());
                    }
                }

                if (chosenOne != null) {
                    boolean updated = this.editCallMsgState(chosenOne.getId(), 1, officeRoomId, doctorId);
                    if (updated) {
                        // 返回最新实体（含绑定后的诊室字段）
                        return callListRepository.findById(chosenOne.getId()).orElse(chosenOne);
                    }
                    log.error("自动调度选中患者 {}，但更新状态失败", chosenOne.getId());
                }
                return null;
            } finally {
                if (deptLock.isHeldByCurrentThread()) {
                    deptLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("自动呼叫锁失败", e);
            return null;
        }
    }

    @Override
    public List<Map<String, String>> getDoctorsByDeptForUi(String deptId) {
        return doctorRepository.findByDeptIdOrderByTitleAsc(deptId).stream().map(dr -> {
            String label = dr.getName() != null && !dr.getName().isEmpty() ? dr.getName() : safeStr(dr.getWorkNo());
            return Map.of("UserName", label);
        }).collect(Collectors.toList());
    }

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }

    @Override
    public boolean registerCall(Map<String, Object> body) {
        try {
            String deptId = resolveDeptIdToken(body.get("DeptID"));
            if (deptId.isEmpty()) {
                return false;
            }
            CallList c = new CallList();
            c.setPatientCode(str(body.get("PatientCode")));
            c.setPatientName(str(body.get("PatientName")));
            c.setDeptID(deptId);
            c.setDoctors(str(body.get("Doctors")));
            c.setXhlb(str(body.get("xhlb")));
            c.setState(0);
            c.setIsForzen(0);
            c.setCallDate(new Date());
            c.setTriageState("WAITING");
            c.setCallTime(parseFlexibleDate(body.get("CallTime")));
            String officeRoomId = str(body.get("OfficeRoomId"));
            if (officeRoomId == null || officeRoomId.isEmpty()) {
                officeRoomId = str(body.get("officeRoomId"));
            }
            if (officeRoomId != null && !officeRoomId.isEmpty()) {
                c.setOfficeRoomId(officeRoomId);
                officeRoomRepository.findById(officeRoomId).ifPresent(room -> c.setOutPatientOffice(room.getName()));
            } else {
                departmentRepository.findById(deptId).ifPresent(d -> c.setOutPatientOffice(d.getName()));
            }
            Integer maxXh = callListRepository.findMaxXhTodayByDept(deptId);
            int next = (maxXh == null ? 0 : maxXh) + 1;
            c.setXh(next);
            c.setShowNo(String.valueOf(next));
            c.setSflbbigtype("OUTPATIENT");
            callListRepository.save(c);
            return true;
        } catch (Exception e) {
            log.warn("registerCall failed", e);
            return false;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Date parseFlexibleDate(Object raw) {
        if (raw == null) {
            return new Date();
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return new Date();
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p).parse(s);
            } catch (ParseException ignored) {
                // try next
            }
        }
        return new Date();
    }

    @Override
    public boolean removeCall(Integer id) {
        if (id == null) {
            return false;
        }
        try {
            callListRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean editorCall(Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return false;
        }
        int id = Integer.parseInt(idObj.toString());
        Optional<CallList> opt = callListRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        CallList c = opt.get();
        if (body.get("PatientCode") != null) {
            c.setPatientCode(str(body.get("PatientCode")));
        }
        if (body.get("PatientName") != null) {
            c.setPatientName(str(body.get("PatientName")));
        }
        if (body.get("CallTime") != null) {
            c.setCallTime(parseFlexibleDate(body.get("CallTime")));
        }
        if (body.get("sflbmc") != null) {
            c.setSflbmc(str(body.get("sflbmc")));
        }
        if (body.get("xh") != null && !str(body.get("xh")).isEmpty()) {
            c.setXh(Integer.parseInt(str(body.get("xh"))));
        }
        if (body.get("xhlb") != null) {
            c.setXhlb(str(body.get("xhlb")));
        }
        if (body.get("DeptID") != null) {
            String nd = resolveDeptIdToken(body.get("DeptID"));
            c.setDeptID(nd);
            departmentRepository.findById(nd).ifPresent(d -> c.setOutPatientOffice(d.getName()));
        }
        if (body.get("Doctors") != null) {
            c.setDoctors(str(body.get("Doctors")));
        }
        if (body.get("remark") != null) {
            c.setRemark(str(body.get("remark")));
        }
        if (body.get("state") != null && !str(body.get("state")).isEmpty()) {
            c.setState(Integer.parseInt(str(body.get("state"))));
        }
        if (body.get("pym") != null) {
            c.setPym(str(body.get("pym")));
        }
        if (body.get("sflbbigtype") != null) {
            c.setSflbbigtype(str(body.get("sflbbigtype")));
        }
        callListRepository.save(c);
        return true;
    }

    @Override
    @Transactional
    public int batchRemove(Map<String, Object> body) {
        List<Integer> ids = collectBatchRemoveIds(body);
        if (ids.isEmpty()) {
            return 0;
        }
        return callListRepository.deleteByIds(ids);
    }

    private List<Integer> collectBatchRemoveIds(Map<String, Object> body) {
        List<Integer> states = new ArrayList<>();
        if (Boolean.TRUE.equals(body.get("state0"))) {
            states.add(0);
        }
        if (Boolean.TRUE.equals(body.get("state1"))) {
            states.add(1);
        }
        // 就诊完成：新口径 4 + 旧口径 2
        if (Boolean.TRUE.equals(body.get("state2"))) {
            states.add(2);
            states.add(4);
        }
        // 已过号：新口径 6 + 旧口径 3
        if (Boolean.TRUE.equals(body.get("state3"))) {
            states.add(3);
            states.add(6);
        }
        if (states.isEmpty()) {
            return List.of();
        }
        Specification<CallList> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            String deptId = resolveDeptIdToken(body.get("DeptID"));
            if (!deptId.isEmpty()) {
                preds.add(cb.equal(root.get("deptID"), deptId));
            }
            String doctors = str(body.get("Doctors"));
            if (!doctors.isEmpty()) {
                preds.add(cb.equal(root.get("doctors"), doctors));
            }
            preds.add(root.get("state").in(states));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return callListRepository.findAll(spec).stream().map(CallList::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean oneclickClear() {
        callListRepository.deleteAllTodayRows();
        return true;
    }

    @Override
    public boolean sortCall(Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return false;
        }
        int id = Integer.parseInt(idObj.toString());
        Optional<CallList> opt = callListRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        CallList c = opt.get();
        if (body.get("xh") != null && !str(body.get("xh")).isEmpty()) {
            c.setXh(Integer.parseInt(str(body.get("xh"))));
        }
        if (body.get("xhlb") != null) {
            c.setXhlb(str(body.get("xhlb")));
        }
        callListRepository.save(c);
        return true;
    }

    /**
     * 体检叫号增强：在现有 GetDocCallList 上附带当前项目与下一站，不另开接口。
     */
    private void attachExamHints(List<CallList> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (CallList call : list) {
            if (call.getPatientCode() == null || call.getPatientCode().isBlank()) {
                continue;
            }
            boolean checkup = call.getSourceType() != null
                    && "HEALTH_CHECKUP".equalsIgnoreCase(call.getSourceType());
            if (!checkup) {
                continue;
            }
            List<com.gocent.pdjh.entity.ExamRecordDetail> records =
                    examRecordDetailRepository.findByPatientCode(call.getPatientCode());
            if (records == null || records.isEmpty()) {
                continue;
            }
            List<Integer> itemIds = records.stream()
                    .map(com.gocent.pdjh.entity.ExamRecordDetail::getItemId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, com.gocent.pdjh.entity.DictExamItem> dictMap = itemIds.isEmpty()
                    ? Map.of()
                    : dictExamItemRepository.findAllById(itemIds).stream()
                    .collect(Collectors.toMap(com.gocent.pdjh.entity.DictExamItem::getId, d -> d, (a, b) -> a));

            String currentName = null;
            List<String> nextNames = new ArrayList<>();
            for (com.gocent.pdjh.entity.ExamRecordDetail d : records) {
                com.gocent.pdjh.entity.DictExamItem dict = dictMap.get(d.getItemId());
                String name = dict != null && dict.getItemName() != null ? dict.getItemName() : ("项目" + d.getItemId());
                int st = d.getState() == null ? 0 : d.getState();
                boolean sameDept = dict != null && call.getDeptID() != null
                        && call.getDeptID().equals(dict.getAssociatedDeptId());
                if ((st == 1 || st == 2) && sameDept && currentName == null) {
                    currentName = name;
                } else if (st == 0) {
                    nextNames.add(name);
                }
            }
            if (currentName == null && call.getRemark() != null && !call.getRemark().isBlank()) {
                currentName = call.getRemark();
            }
            call.setExamItemName(currentName);
            if (!nextNames.isEmpty()) {
                call.setExamNextHint("下一站 " + String.join("、", nextNames.subList(0, Math.min(3, nextNames.size()))));
            } else if (currentName != null) {
                call.setExamNextHint("本项完成后自动派下一站");
            }
        }
    }
}
