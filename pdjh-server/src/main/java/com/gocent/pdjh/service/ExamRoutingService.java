package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.DictExamItem;
import com.gocent.pdjh.entity.ExamRecordDetail;
import com.gocent.pdjh.event.ExamItemCompletedEvent;
import com.gocent.pdjh.event.PatientSignInEvent;
import com.gocent.pdjh.repository.DictExamItemRepository;
import com.gocent.pdjh.repository.ExamRecordDetailRepository;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExamRoutingService {

    private final ExamRecordDetailRepository recordDetailRepository;
    private final DictExamItemRepository dictExamItemRepository;
    private final QueueService queueService;
    private final CallListRepository callListRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 当患者首次扫码签到时，触发算路分配
    @Async
    @EventListener
    @org.springframework.transaction.annotation.Transactional
    public void handlePatientSignIn(PatientSignInEvent event) {
        log.info("【智能导检】收到患者签到事件, 准备初次派单: patientCode={}", event.getPatientCode());
        routeNextExamItem(event.getPatientCode(), "DEFAULT_BATCH");
    }

    // 当患者完成某项体检时，触发下一项的算路分配
    @Async
    @EventListener
    @org.springframework.transaction.annotation.Transactional
    public void handleExamItemCompleted(ExamItemCompletedEvent event) {
        log.info("【智能导检】收到患者项目完成事件, 准备派发下一单: patientCode={}, completedItemId={}",
                event.getPatientCode(), event.getCompletedItemId());
        Integer itemId = event.getCompletedItemId();
        if (itemId == null || itemId <= 0) {
            log.warn("【智能导检】完成事件缺少有效项目 ID，不自动派单。patientCode={}", event.getPatientCode());
            return;
        }
        ExamRecordDetail target = recordDetailRepository.findByPatientCode(event.getPatientCode()).stream()
                .filter(d -> itemId.equals(d.getItemId()))
                .filter(d -> d.getState() == null || d.getState() != 3)
                .findFirst().orElse(null);
        if (target != null) {
            markExamCompleted(target);
        }
        routeNextExamItem(event.getPatientCode(), event.getExamBatchNo());
    }

    /**
     * 叫号完成（call_list.state=4）后回写体检项目并派下一站。
     * 仅 HEALTH_CHECKUP；门诊/药房完成不进导检，避免误入队。
     */
    @org.springframework.transaction.annotation.Transactional
    public void completeFinishedCall(CallList call) {
        if (call == null || call.getPatientCode() == null || call.getPatientCode().isBlank()) {
            return;
        }
        if (call.getSourceType() == null || !"HEALTH_CHECKUP".equalsIgnoreCase(call.getSourceType())) {
            return;
        }
        ExamRecordDetail target = findInProgressItem(call.getPatientCode(), call.getDeptID(), call.getRemark());
        if (target == null) {
            log.warn("【智能导检】体检排队单 {} 已完成，但没有进行中的项目可回写，不派下一站", call.getId());
            return;
        }
        markExamCompleted(target);
        String batch = target.getExamBatchNo() != null && !target.getExamBatchNo().isBlank()
                ? target.getExamBatchNo() : "DEFAULT";
        routeNextExamItem(call.getPatientCode(), batch);
    }

    /**
     * LIS/RIS 回写检查进度。state=4 表示该项目已完成，回写后派下一站。
     * itemCode 为字典 id 或项目名称。
     * @return null 成功（含幂等）；非 null 为失败原因，供 webhook 回给院方
     */
    @org.springframework.transaction.annotation.Transactional
    public String applyExternalExamStatus(String patientCode, String itemCode, Integer state) {
        if (patientCode == null || patientCode.isBlank()) {
            return "patientCode 不能为空";
        }
        if (state == null || state != 4) {
            log.info("【智能导检】外部回写患者 {} 项目 {} 状态 {}，未完成不派下一站", patientCode, itemCode, state);
            return null;
        }
        ExamRecordDetail target = matchExternalItem(patientCode, itemCode);
        if (target == null) {
            return "未找到该患者的体检项目";
        }
        if (Integer.valueOf(3).equals(target.getState())) {
            log.info("【智能导检】患者 {} 项目 {} 已完成，忽略重复回写", patientCode, itemCode);
            return null;
        }
        markExamCompleted(target);
        String batch = target.getExamBatchNo() != null && !target.getExamBatchNo().isBlank()
                ? target.getExamBatchNo() : "DEFAULT";
        routeNextExamItem(patientCode, batch);
        return null;
    }

    private void markExamCompleted(ExamRecordDetail detail) {
        detail.setState(3);
        detail.setCompleteTime(new Date());
        recordDetailRepository.save(detail);
    }

    /** 进行中项目：优先同科室，其次备注里的项目名 */
    private ExamRecordDetail findInProgressItem(String patientCode, String deptId, String remark) {
        List<ExamRecordDetail> records = recordDetailRepository.findByPatientCode(patientCode);
        if (records == null || records.isEmpty()) {
            return null;
        }
        List<Integer> itemIds = records.stream().map(ExamRecordDetail::getItemId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Integer, DictExamItem> dictMap = dictExamItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(DictExamItem::getId, d -> d, (a, b) -> a));
        ExamRecordDetail onlyOne = null;
        int inProgress = 0;
        for (ExamRecordDetail detail : records) {
            int st = detail.getState() == null ? -1 : detail.getState();
            if (st != 1 && st != 2) {
                continue;
            }
            inProgress++;
            onlyOne = detail;
            DictExamItem dict = dictMap.get(detail.getItemId());
            boolean sameDept = deptId != null && dict != null && deptId.equals(dict.getAssociatedDeptId());
            boolean sameName = remark != null && dict != null && remark.equals(dict.getItemName());
            if (sameDept || sameName) {
                return detail;
            }
        }
        return inProgress == 1 ? onlyOne : null;
    }

    private ExamRecordDetail matchExternalItem(String patientCode, String itemCode) {
        List<ExamRecordDetail> records = recordDetailRepository.findByPatientCode(patientCode);
        if (records == null || records.isEmpty()) {
            return null;
        }
        Integer asId = null;
        if (itemCode != null && !itemCode.isBlank()) {
            try {
                asId = Integer.valueOf(itemCode.trim());
            } catch (NumberFormatException ignored) {
                asId = null;
            }
        }
        List<Integer> itemIds = records.stream().map(ExamRecordDetail::getItemId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Integer, DictExamItem> dictMap = dictExamItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(DictExamItem::getId, d -> d, (a, b) -> a));
        ExamRecordDetail nameMatch = null;
        for (ExamRecordDetail detail : records) {
            if (asId != null && asId.equals(detail.getItemId())) {
                return detail;
            }
            DictExamItem dict = dictMap.get(detail.getItemId());
            if (itemCode != null && dict != null && itemCode.equals(dict.getItemName())) {
                nameMatch = detail;
            }
        }
        return nameMatch;
    }

    /** 入队必须有物理诊室；优先开放中的体检诊室 */
    private String resolveOpenRoomId(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            return null;
        }
        List<OfficeRoom> rooms = officeRoomRepository.findByDeptId(deptId);
        if (rooms == null || rooms.isEmpty()) {
            return null;
        }
        List<OfficeRoom> open = new java.util.ArrayList<>();
        for (OfficeRoom room : rooms) {
            if (room.getState() == null || room.getState() == 1) {
                open.add(room);
            }
        }
        if (open.isEmpty()) {
            open = rooms;
        }
        for (OfficeRoom room : open) {
            if (room.getBusinessType() != null && "EXAM".equalsIgnoreCase(room.getBusinessType())) {
                return room.getId();
            }
        }
        return open.get(0).getId();
    }

    private String resolvePatientName(String patientCode) {
        CallList latest = callListRepository.findTodayLatestByPatientCode(patientCode);
        if (latest != null && latest.getPatientName() != null && !latest.getPatientName().isBlank()) {
            String name = latest.getPatientName();
            if (!name.startsWith("体检者-") && !name.startsWith("强权-")) {
                return name;
            }
        }
        return "体检者-" + patientCode;
    }

    @org.springframework.transaction.annotation.Transactional
    public void suspendDept(String deptId) {
        log.info("【危机容错】接收到科室挂起指令: deptId={}", deptId);
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept != null) {
            dept.setIsSuspended(1);
            departmentRepository.save(dept);
        } else {
            log.warn("未找到科室: {}", deptId);
            return;
        }

        List<CallList> waitings = callListRepository.findTodayCallListByDeptAndState(deptId, 0);
        Set<String> affectedPatients = waitings.stream()
            .map(CallList::getPatientCode)
            .collect(Collectors.toSet());

        for (CallList call : waitings) {
            call.setState(4); 
        }
        callListRepository.saveAll(waitings);

        for (String patientCode : affectedPatients) {
            List<ExamRecordDetail> activeItems = recordDetailRepository.findByPatientCodeAndState(patientCode, 1);
            for (ExamRecordDetail detail : activeItems) {
                DictExamItem dict = dictExamItemRepository.findById(detail.getItemId()).orElse(null);
                if (dict != null && deptId.equals(dict.getAssociatedDeptId())) {
                    detail.setState(0);
                    recordDetailRepository.save(detail);

                    log.info("【危机容错】为受影响患者重新算路: patientCode={}, 恢复待查项目={}", patientCode, dict.getItemName());
                    routeNextExamItem(patientCode, "EMERGENCY_REROUTE");
                }
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void forceRoute(String patientCode, Integer targetItemId, boolean isPriority) {
        log.info("【护士强权】对患者 {} 执行强签至项目 {}, 置顶特权={}", patientCode, targetItemId, isPriority);
        
        List<ExamRecordDetail> activeItems = recordDetailRepository.findByPatientCodeAndState(patientCode, 1);
        for (ExamRecordDetail detail : activeItems) {
            detail.setState(0);
            recordDetailRepository.save(detail);
        }
        
        List<CallList> activeCalls = callListRepository.findByPatientCodeAndState(patientCode, 0);
        for (CallList call : activeCalls) {
            call.setState(4);
        }
        callListRepository.saveAll(activeCalls);

        ExamRecordDetail targetDetail = recordDetailRepository.findByPatientCode(patientCode).stream()
                .filter(d -> d.getItemId().equals(targetItemId))
                .findFirst().orElse(null);
                
        if (targetDetail == null) {
            log.warn("【护士强权】失败: 患者 {} 并没有体检项目 {}", patientCode, targetItemId);
            return;
        }
        
        DictExamItem dict = dictExamItemRepository.findById(targetItemId).orElse(null);
        if (dict == null || dict.getAssociatedDeptId() == null) {
            log.warn("【护士强权】失败: 字典配置不全. ItemId={}", targetItemId);
            return;
        }

        String roomId = resolveOpenRoomId(dict.getAssociatedDeptId());
        if (roomId == null) {
            log.warn("【护士强权】失败: 科室 {} 没有可用诊室，无法入队", dict.getAssociatedDeptId());
            return;
        }
        EnqueueRequest req = new EnqueueRequest();
        req.setPatientCode(patientCode);
        req.setPatientName(resolvePatientName(patientCode));
        req.setSex("0");
        req.setAgeStr("");
        req.setDeptID(dict.getAssociatedDeptId());
        req.setOfficeRoomId(roomId);
        req.setRequireSignIn(false);

        if (isPriority) {
            req.setIsAppointment(2); 
            req.setPriority(9999);
        }

        queueService.enqueue(req);
        
        targetDetail.setState(1);
        recordDetailRepository.save(targetDetail);
        log.info("【护士强权】执行完毕，已将大空降兵送入 {} 的排队列表顶部", dict.getAssociatedDeptId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void recalculateRoute(String patientCode) {
        log.info("【智能导检】开始为患者强制重新算路: {}", patientCode);
        
        List<ExamRecordDetail> activeItems = recordDetailRepository.findByPatientCodeAndState(patientCode, 1);
        for (ExamRecordDetail detail : activeItems) {
            detail.setState(0);
            recordDetailRepository.save(detail);
        }
        
        List<CallList> activeCalls = callListRepository.findByPatientCodeAndState(patientCode, 0);
        for (CallList call : activeCalls) {
            call.setState(4); // 作废当前排单
        }
        callListRepository.saveAll(activeCalls);

        routeNextExamItem(patientCode, "MANUAL_RECALCULATE");
    }

    public synchronized void routeNextExamItem(String patientCode, String batchNo) {
        List<ExamRecordDetail> pendingItems = recordDetailRepository.findByPatientCodeAndState(patientCode, 0);

        if (pendingItems == null || pendingItems.isEmpty()) {
            log.info("【智能导检】患者 {} 的所有体检项目已排完或已完成，流程结束。", patientCode);
            return;
        }

        List<Integer> itemIds = pendingItems.stream()
                .map(ExamRecordDetail::getItemId)
                .collect(Collectors.toList());
        List<DictExamItem> dictList = dictExamItemRepository.findAllById(itemIds);
        java.util.Map<Integer, DictExamItem> dictMap = dictList.stream()
                .collect(Collectors.toMap(DictExamItem::getId, d -> d));

        List<ExamRecordDetail> availableItems = new java.util.ArrayList<>();
        List<ExamRecordDetail> allPatientItems = recordDetailRepository.findByPatientCode(patientCode);

        for (ExamRecordDetail detail : pendingItems) {
            DictExamItem dict = dictMap.get(detail.getItemId());
            if (dict == null || dict.getAssociatedDeptId() == null) continue;

            Department dept = departmentRepository.findById(dict.getAssociatedDeptId()).orElse(null);
            if (dept != null && Integer.valueOf(1).equals(dept.getIsSuspended())) {
                continue;
            }

            if (dict.getDependsOnIds() != null && !dict.getDependsOnIds().trim().isEmpty()) {
                List<Integer> deps = Arrays.stream(dict.getDependsOnIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).collect(Collectors.toList());
                boolean allDepsMet = true;
                for (Integer depId : deps) {
                    ExamRecordDetail depDetail = allPatientItems.stream()
                            .filter(d -> d.getItemId().equals(depId)).findFirst().orElse(null);
                    if (depDetail != null && depDetail.getState() != 3) {
                        allDepsMet = false;
                        break;
                    }
                }
                if (!allDepsMet) {
                    continue; 
                }
            }
            availableItems.add(detail);
        }

        if (availableItems.isEmpty()) {
            log.info("【智能导检】患者 {} 暂时没有可做的体检项目（可能全部相关科室均宕机或前置项目都在阻塞中）", patientCode);
            return;
        }

        // 读取前端 Vue 存入的全局体检算法配置 RULE_EXAM
        String triageAlgorithm = "FASTING_FIRST"; // 默认安全退化策略
        SysConfig config = sysConfigService.getConfigByKey("RULE_EXAM");
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(config.getConfigValue());
                if (root.has("triageAlgorithm")) {
                    triageAlgorithm = root.get("triageAlgorithm").asText();
                } else if (root.has("routingAlgorithm")) {
                    triageAlgorithm = root.get("routingAlgorithm").asText();
                }
                
                // P1-A: 分时段自动切换算法 (Override if enabled and matched)
                if (root.has("timeScheduleEnabled") && root.get("timeScheduleEnabled").asBoolean()) {
                    if (root.has("timeSchedules") && root.get("timeSchedules").isArray()) {
                        java.time.LocalTime nowTime = java.time.LocalTime.now();
                        for (JsonNode schedule : root.get("timeSchedules")) {
                            if (schedule.has("startTime") && schedule.has("endTime") && schedule.has("algorithm")) {
                                String startStr = schedule.get("startTime").asText();
                                String endStr = schedule.get("endTime").asText();
                                if (startStr != null && !startStr.isEmpty() && endStr != null && !endStr.isEmpty()) {
                                    try {
                                        java.time.LocalTime startTime = java.time.LocalTime.parse(startStr);
                                        java.time.LocalTime endTime = java.time.LocalTime.parse(endStr);
                                        // 检查是否在时间段内 [startTime, endTime)
                                        if (!nowTime.isBefore(startTime) && nowTime.isBefore(endTime)) {
                                            triageAlgorithm = schedule.get("algorithm").asText();
                                            log.info("【智能导检】命中分时计划 ({}~{})，临时覆盖算法为: {}", startStr, endStr, triageAlgorithm);
                                            break; // 命中匹配的时间段后即跳出
                                        }
                                    } catch (Exception timeEx) {
                                        log.warn("【智能导检】解析分时计划时间格式失败: {} - {}", startStr, endStr);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("【智能导检】解析 RULE_EXAM 配置失败: {}", e.getMessage());
            }
        }
        
        log.info("【智能导检】患者 {} 当前选用路由算法策略: {}", patientCode, triageAlgorithm);

        if ("NONE".equals(triageAlgorithm)) {
            log.info("【智能导检】当前采用 NONE 退化模式，拒绝自动分配下一站！患者需人工干预。");
            return;
        }

        ExamRecordDetail targetItem = null;
        DictExamItem targetDict = null;

        if ("LEAST_WAIT".equals(triageAlgorithm) || "SHORTEST_PATH".equals(triageAlgorithm)) {
            // [最少排队优先] 或 [最短动线退化映射] -> 找排队人数最少的直接去
            int minWait = Integer.MAX_VALUE;
            for (ExamRecordDetail detail : availableItems) {
                DictExamItem dict = dictMap.get(detail.getItemId());
                if (dict != null && dict.getAssociatedDeptId() != null) {
                    List<CallList> waitings = callListRepository.findTodayCallListByDeptAndState(dict.getAssociatedDeptId(), 0);
                    int currentWait = waitings == null ? 0 : waitings.size();
                    if (currentWait < minWait) {
                        minWait = currentWait;
                        targetItem = detail;
                        targetDict = dict;
                    }
                }
            }
        } else {
            // 传统 FASTING_FIRST 空腹强优先
            for (ExamRecordDetail detail : availableItems) {
                DictExamItem dict = dictMap.get(detail.getItemId());
                if (dict != null && dict.getIsFasting() != null && dict.getIsFasting() == 1) {
                    targetItem = detail;
                    targetDict = dict;
                    break;
                }
            }
        }

        if (targetItem == null) {
            targetItem = availableItems.get(0);
            targetDict = dictMap.get(targetItem.getItemId());
        }

        if (targetDict == null || targetDict.getAssociatedDeptId() == null) {
            log.warn("【智能导检】派发项目失败: 未找到可用项目或字典配置缺失");
            return;
        }

        log.info("【智能导检】计算流转完成，为患者 {} 派发下一项目: {}", patientCode, targetDict.getItemName());

        String roomId = resolveOpenRoomId(targetDict.getAssociatedDeptId());
        if (roomId == null) {
            log.error("【智能导检】科室 {} 没有可用诊室，项目 {} 无法入队", targetDict.getAssociatedDeptId(), targetDict.getItemName());
            return;
        }

        EnqueueRequest req = new EnqueueRequest();
        req.setPatientCode(patientCode);
        req.setPatientName(resolvePatientName(patientCode));
        req.setSex("0");
        req.setAgeStr("");
        req.setDeptID(targetDict.getAssociatedDeptId());
        req.setOfficeRoomId(roomId);
        req.setRequireSignIn(false);
        // P1-2b: 体检导检入队统一标记来源
        req.setSourceType("HEALTH_CHECKUP");

        CallList savedCallList = queueService.enqueue(req);
        savedCallList.setRemark(targetDict.getItemName());
        savedCallList.setSflbmc("体检");
        callListRepository.save(savedCallList);

        targetItem.setState(1);
        recordDetailRepository.save(targetItem);
        
        log.info("【智能导检】推入目标科室队列成功，排队池记录ID: {}, sourceType=HEALTH_CHECKUP", savedCallList.getId());
    }
}
