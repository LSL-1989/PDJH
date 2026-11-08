package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.MedTechSlot;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.MedTechSlotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * P1-1b：医技科室排队调度引擎
 * 支持检验科、超声科、放射科、心电图室的差异化排队逻辑
 * 策略来源：sys_config.RULE_MEDICAL_TECH（各科室独立 JSON 配置块）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedTechQueueService {

    private final CallListRepository callListRepository;
    private final DepartmentRepository departmentRepository;
    private final MedTechSlotRepository slotRepository;
    private final SysConfigService sysConfigService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================================================
    // 核心调度入口：根据 medTechSubType 分发不同策略
    // ===================================================

    /**
     * 选出下一个应被叫号的患者
     * @param deptId 医技科室 ID
     * @return 选中的 CallList，如无人等待则返回 null
     */
    public CallList selectNextPatient(String deptId) {
        Optional<Department> deptOpt = departmentRepository.findById(deptId);
        if (deptOpt.isEmpty()) {
            log.warn("【医技调度】科室 {} 不存在", deptId);
            return null;
        }
        Department dept = deptOpt.get();
        String subType = dept.getMedTechSubType();
        if (subType == null) subType = "LAB"; // 默认降级为检验科策略

        log.info("【医技调度】科室 {} 子类型={}, 开始选号", deptId, subType);

        return switch (subType) {
            case "LAB"        -> selectForLab(deptId);
            case "ULTRASOUND" -> selectForUltrasound(deptId);
            case "RADIOLOGY"  -> selectForRadiology(deptId);
            case "ECG"        -> selectForEcg(deptId);
            default           -> selectForLab(deptId);
        };
    }

    // ===================================================
    // 检验科：急诊绿色通道 + 最少排队窗口分流
    // ===================================================
    private CallList selectForLab(String deptId) {
        JsonNode cfg = readSubConfig("lab");

        // 1. 优先：急诊/危重（isAppointment=9）
        List<CallList> urgentQueue = callListRepository.findTodayCallListByDeptAndState(deptId, 0)
            .stream().filter(c -> Integer.valueOf(9).equals(c.getIsAppointment()))
            .sorted((a, b) -> a.getXh() - b.getXh())
            .toList();
        if (!urgentQueue.isEmpty()) {
            log.info("【检验科/急诊通道】选出急诊患者: {}", urgentQueue.get(0).getPatientName());
            return urgentQueue.get(0);
        }

        // 2. 普通队列：按 xh（序号）升序
        List<CallList> normalQueue = callListRepository.findTodayCallListByDeptAndState(deptId, 0)
            .stream().filter(c -> !Integer.valueOf(9).equals(c.getIsAppointment()))
            .sorted((a, b) -> a.getXh() - b.getXh())
            .toList();

        if (normalQueue.isEmpty()) return null;

        // 3. 检验科支持多窗口分流（如有子窗口，按最少排队选）
        boolean multiWindow = cfg != null && cfg.has("multiWindow") && cfg.get("multiWindow").asBoolean(false);
        if (multiWindow) {
            List<Department> windows = departmentRepository.findAll().stream()
                .filter(d -> deptId.equals(d.getFatherID()) && "LAB".equals(d.getMedTechSubType()))
                .toList();
            if (!windows.isEmpty()) {
                // 把普通队列中的第一个人分到负载最小的子窗口
                String targetWindow = windows.stream()
                    .min((a, b) -> {
                        int ca = callListRepository.findTodayCallListByDeptAndState(a.getId(), 0).size();
                        int cb = callListRepository.findTodayCallListByDeptAndState(b.getId(), 0).size();
                        return ca - cb;
                    })
                    .map(Department::getId).orElse(deptId);
                log.info("【检验科/多窗口】将患者 {} 路由到子窗口 {}", normalQueue.get(0).getPatientName(), targetWindow);
                CallList patient = normalQueue.get(0);
                patient.setDeptID(targetWindow);
                callListRepository.save(patient);
            }
        }

        return normalQueue.get(0);
    }

    // ===================================================
    // 超声科：预约号优先 + 权重系数排序
    // ===================================================
    private CallList selectForUltrasound(String deptId) {
        JsonNode cfg = readSubConfig("ultrasound");
        int appointmentWeight = cfg != null && cfg.has("appointmentWeight")
            ? cfg.get("appointmentWeight").asInt(3) : 3;

        List<CallList> queue = callListRepository.findTodayCallListByDeptAndState(deptId, 0);
        if (queue.isEmpty()) return null;

        // 计算有效排位：预约号 xh 按权重压缩（xh / appointmentWeight），现场号直接用 xh
        CallList best = queue.stream().min((a, b) -> {
            int xhA = a.getXh() != null ? a.getXh() : 9999;
            int xhB = b.getXh() != null ? b.getXh() : 9999;
            boolean apptA = Integer.valueOf(1).equals(a.getIsAppointment());
            boolean apptB = Integer.valueOf(1).equals(b.getIsAppointment());
            int effA = apptA ? xhA / appointmentWeight : xhA;
            int effB = apptB ? xhB / appointmentWeight : xhB;
            return effA - effB;
        }).orElse(null);

        if (best != null) {
            log.info("【超声科/预约优先】选出患者: {}, 预约={}, xh={}",
                best.getPatientName(), best.getIsAppointment(), best.getXh());
        }
        return best;
    }

    // ===================================================
    // 放射科（CT/MRI）：时段配额验证
    // ===================================================
    private CallList selectForRadiology(String deptId) {
        List<CallList> queue = callListRepository.findTodayCallListByDeptAndState(deptId, 0)
            .stream().sorted((a, b) -> {
                int xhA = a.getXh() != null ? a.getXh() : 9999;
                int xhB = b.getXh() != null ? b.getXh() : 9999;
                return xhA - xhB;
            }).toList();

        if (queue.isEmpty()) return null;

        // 对于放射科，验证时段配额是否还有余量
        LocalDate today = LocalDate.now();
        List<MedTechSlot> available = slotRepository.findAvailableSlots(deptId, today);
        if (available.isEmpty()) {
            log.warn("【放射科/时段验证】科室 {} 今日时段配额已耗尽，暂停叫号", deptId);
            // 广播配额耗尽告警
            messagingTemplate.convertAndSend("/topic/hall", (Object) java.util.Map.of(
                "type", "SLOT_EXHAUSTED",
                "deptId", deptId,
                "message", "放射科今日预约时段已满"
            ));
            return null;
        }

        // 满足配额，取序列首个患者
        CallList next = queue.get(0);
        // 消耗最近可用时段的一个配额
        MedTechSlot slot = available.get(0);
        int updated = slotRepository.incrementBooked(slot.getId());
        if (updated == 0) {
            log.warn("【放射科/时段验证】时段 {} 竞争写入失败（并发），跳过本次叫号", slot.getTimeSlot());
            return null;
        }
        log.info("【放射科/时段配额】消耗 {} 时段 {} 配额，叫号患者: {}",
            slot.getSlotDate(), slot.getTimeSlot(), next.getPatientName());
        return next;
    }

    // ===================================================
    // 心电图室：急诊10分钟绿色通道 + 普通先来先叫
    // ===================================================
    private CallList selectForEcg(String deptId) {
        JsonNode cfg = readSubConfig("ecg");
        int emergencyMinutes = cfg != null && cfg.has("emergencyMinutes")
            ? cfg.get("emergencyMinutes").asInt(10) : 10;

        List<CallList> queue = callListRepository.findTodayCallListByDeptAndState(deptId, 0);
        if (queue.isEmpty()) return null;

        Date emergencyCutoff = new Date(System.currentTimeMillis() - (emergencyMinutes * 60 * 1000L));

        // 急诊：isAppointment=9 或 callTime 超过急诊等待阈值的置顶
        List<CallList> emergencyQueue = queue.stream()
            .filter(c -> Integer.valueOf(9).equals(c.getIsAppointment())
                || (c.getCallTime() != null && c.getCallTime().before(emergencyCutoff)))
            .sorted((a, b) -> a.getXh() - b.getXh())
            .toList();

        if (!emergencyQueue.isEmpty()) {
            log.info("【心电图室/急诊通道】选出急诊/超时患者: {}", emergencyQueue.get(0).getPatientName());
            return emergencyQueue.get(0);
        }

        // 普通：按 xh 升序
        return queue.stream()
            .sorted((a, b) -> {
                int xhA = a.getXh() != null ? a.getXh() : 9999;
                int xhB = b.getXh() != null ? b.getXh() : 9999;
                return xhA - xhB;
            }).findFirst().orElse(null);
    }

    // ===================================================
    // 预约时段查询（供前端使用）
    // ===================================================

    /** 查询指定科室当天可用时段列表 */
    public List<MedTechSlot> getAvailableSlots(String deptId, LocalDate date) {
        return slotRepository.findAvailableSlots(deptId, date);
    }

    /** 查询指定科室、日期全部时段（含已满） */
    public List<MedTechSlot> getAllSlots(String deptId, LocalDate date) {
        return slotRepository.findByDeptIdAndSlotDateOrderByTimeSlotAsc(deptId, date);
    }

    /**
     * 按时间网格批量创建空时段（已存在的时段跳过）。
     * @return 新建条数
     */
    @Transactional
    public int createEmptySlotsIfAbsent(
            String deptId,
            LocalDate date,
            int startH,
            int startM,
            int endH,
            int endM,
            int intervalMin,
            int capacity) {
        int created = 0;
        int curH = startH;
        int curM = startM;
        while (curH < endH || (curH == endH && curM < endM)) {
            String slot = String.format("%02d:%02d", curH, curM);
            if (slotRepository.findByDeptIdAndSlotDateAndTimeSlot(deptId, date, slot).isEmpty()) {
                MedTechSlot s = new MedTechSlot();
                s.setDeptId(deptId);
                s.setSlotDate(date);
                s.setTimeSlot(slot);
                s.setCapacity(capacity);
                s.setBooked(0);
                slotRepository.save(s);
                created++;
            }
            curM += intervalMin;
            while (curM >= 60) {
                curM -= 60;
                curH++;
            }
        }
        return created;
    }

    /**
     * 预约某个时段
     * @return true=预约成功, false=配额已满或并发写入失败
     */
    @Transactional
    public boolean bookSlot(String deptId, LocalDate date, String timeSlot) {
        Optional<MedTechSlot> slotOpt = slotRepository.findByDeptIdAndSlotDateAndTimeSlot(deptId, date, timeSlot);
        if (slotOpt.isEmpty()) {
            // 时段不存在时自动创建（走默认配额2）
            MedTechSlot newSlot = new MedTechSlot();
            newSlot.setDeptId(deptId);
            newSlot.setSlotDate(date);
            newSlot.setTimeSlot(timeSlot);
            newSlot.setCapacity(2);
            newSlot.setBooked(1);
            slotRepository.save(newSlot);
            log.info("【预约时段】自动创建时段 {}@{}-{} 并完成首次预约", deptId, date, timeSlot);
            return true;
        }
        MedTechSlot slot = slotOpt.get();
        if (slot.getBooked() >= slot.getCapacity()) {
            log.warn("【预约时段】时段 {}@{}-{} 配额已满({}/{})", deptId, date, timeSlot, slot.getBooked(), slot.getCapacity());
            return false;
        }
        int result = slotRepository.incrementBooked(slot.getId());
        return result > 0;
    }

    // ===================================================
    // 内部：读取 RULE_MEDICAL_TECH 中指定科室配置块
    // ===================================================
    private JsonNode readSubConfig(String subType) {
        SysConfig config = sysConfigService.getConfigByKey("RULE_MEDICAL_TECH");
        if (config == null || config.getConfigValue() == null) return null;
        try {
            JsonNode root = objectMapper.readTree(config.getConfigValue());
            return root.has(subType) ? root.get(subType) : null;
        } catch (Exception e) {
            log.error("【医技调度】解析 RULE_MEDICAL_TECH 配置失败: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public Map<String, Object> updateSlot(Long id, Map<String, Object> body) {
        Optional<MedTechSlot> opt = slotRepository.findById(id);
        if (opt.isEmpty()) {
            return Map.of("success", false, "message", "时段不存在");
        }
        MedTechSlot slot = opt.get();
        if (body.containsKey("capacity")) {
            slot.setCapacity(Integer.parseInt(body.get("capacity").toString()));
        }
        if (body.containsKey("remark")) {
            slot.setRemark(body.get("remark").toString());
        }
        slotRepository.save(slot);
        return Map.of("success", true, "slot", slot);
    }

    @Transactional
    public Map<String, Object> setSlotEnabled(Long id, boolean enabled) {
        Optional<MedTechSlot> opt = slotRepository.findById(id);
        if (opt.isEmpty()) {
            return Map.of("success", false, "message", "时段不存在");
        }
        MedTechSlot slot = opt.get();
        slot.setIsDisabled(enabled ? 0 : 1);
        slotRepository.save(slot);
        return Map.of("success", true, "slot", slot);
    }
}
