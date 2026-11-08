package com.gocent.pdjh.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueManager {

    private final CallListRepository callListRepository;
    private final DepartmentRepository departmentRepository;
    private final SysConfigService sysConfigService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final com.gocent.pdjh.integration.IntegrationEventPublisher integrationEventPublisher;

    // L1 Cache for immediate memory-speed queue distribution (avoids Redis)
    private final Map<String, List<Long>> inMemoryDeptQueues = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> deptLocks = new ConcurrentHashMap<>();

    /**
     * Atomically adds a patient to a department's physical queue utilizing ReentrantLocks for concurrency.
     */
    public boolean lockAndEnqueue(String deptId, Long queueId) {
        deptLocks.putIfAbsent(deptId, new ReentrantLock());
        ReentrantLock lock = deptLocks.get(deptId);
        
        // Non-blocking try wait or short blocking
        lock.lock();
        try {
            inMemoryDeptQueues.putIfAbsent(deptId, new ArrayList<>());
            List<Long> queue = inMemoryDeptQueues.get(deptId);
            if (!queue.contains(queueId)) {
                queue.add(queueId);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Pops the next patient utilizing lock mechanisms.
     */
    public Long dequeueNext(String deptId) {
        ReentrantLock lock = deptLocks.get(deptId);
        if (lock == null) return null;

        lock.lock();
        try {
            List<Long> queue = inMemoryDeptQueues.get(deptId);
            if (queue != null && !queue.isEmpty()) {
                return queue.remove(0);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 定时任务：扫描全院排队表，对药房业务线下超时的患者执行自动过号
     * 避免因长时间不取药导致大盘拥堵。默认每两分钟扫描一次。
     */
    @Scheduled(fixedRate = 120000)
    public void scanPharmacyTimeout() {
        log.info("【系统调度】开始执行药房滞留任务定时扫描机制...");
        // 读取配置
        int timeoutMinutes = 30; // 默认30分钟过号
        SysConfig config = sysConfigService.getConfigByKey("RULE_PHARMACY");
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(config.getConfigValue());
                if (root.has("timeoutMinutes")) {
                    timeoutMinutes = root.get("timeoutMinutes").asInt(30);
                }
            } catch (Exception e) {
                log.error("【药房调度】解析 RULE_PHARMACY 配置失败: {}", e.getMessage());
            }
        }
        
        long cutoffTimeMillis = System.currentTimeMillis() - (timeoutMinutes * 60 * 1000L);
        Date cutoffDate = new Date(cutoffTimeMillis);

        // 获取所有属于药房的科室列表
        List<Department> allDepts = departmentRepository.findAll();
        List<String> pharmacyDeptIds = allDepts.stream()
                .filter(d -> "PHARMACY".equals(d.getBusinessType()))
                .map(Department::getId)
                .toList();

        if (pharmacyDeptIds.isEmpty()) return;

        // 遍历每个药房的当前等待中（state=0）的患者
        for (String deptId : pharmacyDeptIds) {
            List<CallList> waitingPatients = callListRepository.findPharmacyQueueList(deptId, 0);
            for (CallList patient : waitingPatients) {
                if (patient.getCallTime() != null && patient.getCallTime().before(cutoffDate)) {
                    log.info("【自动踢出】患者 {} 在药房科室 {} 等待超过 {} 分钟，执行自动过号作废处理", 
                             patient.getPatientName(), deptId, timeoutMinutes);
                    patient.setState(4); // 过号或作废
                    patient.setEndDate(new Date());
                    callListRepository.save(patient);
                }
            }
        }
    }

    /**
     * P0-1: 定时任务：扫描门诊待诊超时患者，根据 RULE_OUTPATIENT.callTimeoutMin 自动执行过号
     * 默认每 3 分钟扫描一次，超时阈值从配置读取（默认 10 分钟）
     */
    @Scheduled(fixedRate = 180000)
    public void scanOutpatientTimeout() {
        log.info("【系统调度】开始执行门诊超时过号定时扫描...");
        int timeoutMinutes = 10;
        int maxSkipCount = 3;
        SysConfig config = sysConfigService.getConfigByKey("RULE_OUTPATIENT");
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(config.getConfigValue());
                if (root.has("callTimeoutMin")) timeoutMinutes = root.get("callTimeoutMin").asInt(10);
                if (root.has("maxSkipCount"))   maxSkipCount   = root.get("maxSkipCount").asInt(3);
            } catch (Exception e) {
                log.error("【门诊调度】解析 RULE_OUTPATIENT 配置失败: {}", e.getMessage());
            }
        }
        long cutoff = System.currentTimeMillis() - (timeoutMinutes * 60 * 1000L);
        Date cutoffDate = new Date(cutoff);

        List<Department> allDepts = departmentRepository.findAll();
        List<String> outpatientDeptIds = allDepts.stream()
                .filter(d -> "OUTPATIENT".equals(d.getBusinessType()))
                .map(Department::getId)
                .toList();
        if (outpatientDeptIds.isEmpty()) return;

        int effectiveMaxSkip = maxSkipCount;
        for (String deptId : outpatientDeptIds) {
            List<CallList> waiting = callListRepository.findTodayCallListByDeptAndState(deptId, 0);
            for (CallList patient : waiting) {
                int callCount = patient.getCallCount() != null ? patient.getCallCount() : 0;
                if (callCount == 0) continue; // 尚未被呼叫，不触发超时
                if (patient.getCallTime() != null && patient.getCallTime().before(cutoffDate)) {
                    if (callCount >= effectiveMaxSkip) {
                        log.info("【门诊过号】患者 {} 已被叫号 {} 次且超过 {} 分钟未就诊，执行过号",
                            patient.getPatientName(), callCount, timeoutMinutes);
                        patient.setState(6);
                        patient.setTriageState("PASSED");
                        patient.setEndDate(new Date());
                        java.util.Map<String, Object> payload = java.util.Map.of(
                            "type", "PATIENT_EXPIRED",
                            "callListId", patient.getId(),
                            "patientName", patient.getPatientName()
                        );
                        messagingTemplate.convertAndSend("/topic/hall", (Object) payload);
                        integrationEventPublisher.publishEvent("PATIENT_EXPIRED", payload);
                    }
                }
            }
        }
    }

    /**
     * P0-2: 药房窗口分流算法选择器
     * 对药房父科室下的各子窗口，根据算法选出最优目标窗口 ID
     * @param parentDeptId 药房父科室 ID
     * @param algorithm    SHORTEST_QUEUE | ROUND_ROBIN
     * @return 选中的子窗口科室 ID，若无子窗口则返回原 ID
     */
    public String selectPharmacyWindow(String parentDeptId, String algorithm) {
        List<Department> children = departmentRepository.findAll().stream()
            .filter(d -> parentDeptId.equals(d.getFatherID()) && "PHARMACY".equals(d.getBusinessType()))
            .toList();

        if (children.isEmpty()) {
            log.debug("【药房分流】科室 {} 无子窗口，直接入主队列", parentDeptId);
            return parentDeptId;
        }

        if ("SHORTEST_QUEUE".equals(algorithm)) {
            String bestWindowId = children.get(0).getId();
            int minWait = Integer.MAX_VALUE;
            for (Department window : children) {
                List<CallList> waiting = callListRepository.findTodayCallListByDeptAndState(window.getId(), 0);
                int cnt = waiting == null ? 0 : waiting.size();
                if (cnt < minWait) {
                    minWait = cnt;
                    bestWindowId = window.getId();
                }
            }
            log.info("【药房分流/SHORTEST_QUEUE】父科室 {} 选中窗口 {} (当前负载:{}人)", parentDeptId, bestWindowId, minWait);
            return bestWindowId;
        } else {
            // ROUND_ROBIN: 轮询
            deptLocks.putIfAbsent(parentDeptId + ":rr", new java.util.concurrent.locks.ReentrantLock());
            java.util.concurrent.locks.ReentrantLock lock = deptLocks.get(parentDeptId + ":rr");
            lock.lock();
            try {
                inMemoryDeptQueues.putIfAbsent(parentDeptId + ":rr_counter", new ArrayList<>());
                List<Long> counter = inMemoryDeptQueues.get(parentDeptId + ":rr_counter");
                long current = counter.isEmpty() ? 0L : counter.get(0);
                int idx = (int)(current % children.size());
                counter.clear();
                counter.add(current + 1);
                String selectedId = children.get(idx).getId();
                log.info("【药房分流/ROUND_ROBIN】父科室 {} 第{}次分配至窗口 {}", parentDeptId, current, selectedId);
                return selectedId;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * P1-1f: 定时任务：扫描医技科室（检验/超声/放射/心电）超时待检患者
     * 每 5 分钟扫描一次，各科 callTimeoutMin 独立配置
     */
    @Scheduled(fixedRate = 300000)
    public void scanMedTechTimeout() {
        log.debug("【医技调度】开始执行医技科室超时过号扫描...");

        // 读取各科配置的超时分钟（走 RULE_MEDICAL_TECH 同一配置，各子块独立）
        java.util.Map<String, Integer> subTypeTimeouts = new java.util.HashMap<>();
        subTypeTimeouts.put("LAB",        15);  // 检验科默认15分钟
        subTypeTimeouts.put("ULTRASOUND", 20);  // 超声科默认20分钟
        subTypeTimeouts.put("RADIOLOGY",  30);  // 放射科默认30分钟
        subTypeTimeouts.put("ECG",        10);  // 心电图室默认10分钟

        SysConfig config = sysConfigService.getConfigByKey("RULE_MEDICAL_TECH");
        if (config != null && config.getConfigValue() != null) {
            try {
                JsonNode root = objectMapper.readTree(config.getConfigValue());
                for (String key : List.of("lab", "ultrasound", "radiology", "ecg")) {
                    String mapKey = key.toUpperCase();
                    if (root.has(key) && root.get(key).has("callTimeoutMin")) {
                        subTypeTimeouts.put(mapKey, root.get(key).get("callTimeoutMin").asInt());
                    }
                }
            } catch (Exception e) {
                log.error("【医技调度】解析超时配置失败: {}", e.getMessage());
            }
        }

        List<Department> allDepts = departmentRepository.findAll();
        List<Department> medTechDepts = allDepts.stream()
            .filter(d -> d.getMedTechSubType() != null && !d.getMedTechSubType().isEmpty())
            .toList();

        for (Department dept : medTechDepts) {
            String subType = dept.getMedTechSubType();
            int timeout = subTypeTimeouts.getOrDefault(subType, 20);
            Date cutoff = new Date(System.currentTimeMillis() - (timeout * 60 * 1000L));

            List<CallList> waiting = callListRepository.findTodayCallListByDeptAndState(dept.getId(), 0);
            for (CallList patient : waiting) {
                int callCount = patient.getCallCount() != null ? patient.getCallCount() : 0;
                if (callCount == 0) continue;
                if (patient.getCallTime() != null && patient.getCallTime().before(cutoff)) {
                    log.info("【{}超时过号】患者 {} 叫号{}次超过{}分钟未到检，执行过号",
                        subType, patient.getPatientName(), callCount, timeout);
                    patient.setState(4);
                    patient.setEndDate(new Date());
                    java.util.Map<String, Object> payload = java.util.Map.of(
                        "type", "PATIENT_EXPIRED",
                        "deptId", dept.getId(),
                        "subType", subType,
                        "callListId", patient.getId(),
                        "patientName", patient.getPatientName()
                    );
                    messagingTemplate.convertAndSend("/topic/hall", (Object) payload);
                    integrationEventPublisher.publishEvent("PATIENT_EXPIRED", payload);
                }
            }
        }
    }
}

