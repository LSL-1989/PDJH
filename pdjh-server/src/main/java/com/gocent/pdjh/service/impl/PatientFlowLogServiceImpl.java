package com.gocent.pdjh.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.PatientFlowLog;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.PatientFlowLogRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.PatientFlowLogService;
import com.gocent.pdjh.service.support.PatientFlowDisplayText;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 患者全流程留痕实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientFlowLogServiceImpl implements PatientFlowLogService {

    private final PatientFlowLogRepository patientFlowLogRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void record(CallList call, PatientFlowEvent event, String operatorSource,
                       String operatorNo, String operatorName, String remark, Object payload) {
        if (call == null || event == null) {
            return;
        }
        recordRaw(call.getId(), call.getPatientCode(), call.getPatientName(), call.getShowNo(),
                call.getDeptID(), event, operatorSource, operatorNo, operatorName, remark, payload);
    }

    @Override
    @Async
    public void recordRaw(Integer callListId, String patientCode, String patientName, String showNo,
                          String deptId, PatientFlowEvent event, String operatorSource,
                          String operatorNo, String operatorName, String remark, Object payload) {
        if (event == null) {
            return;
        }
        try {
            PatientFlowLog row = new PatientFlowLog();
            row.setCallListId(callListId);
            row.setPatientCode(patientCode);
            row.setPatientName(patientName);
            row.setShowNo(showNo);
            row.setDeptId(deptId);
            row.setEventType(event.getCode());
            row.setEventLabel(event.getLabel());
            row.setOperatorSource(operatorSource != null ? operatorSource : "SYS");
            row.setOperatorNo(operatorNo);
            row.setOperatorName(operatorName);
            row.setRemark(remark);
            if (payload != null) {
                if (payload instanceof String) {
                    row.setPayload((String) payload);
                } else {
                    row.setPayload(objectMapper.writeValueAsString(payload));
                }
            }
            patientFlowLogRepository.save(row);
        } catch (Exception e) {
            log.error("患者流程日志写入失败 event={} callListId={}: {}",
                    event, callListId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> queryPage(int pageIndex, int pageSize, String keyword, String eventType,
                                         String patientCode, Integer callListId, Date startTime, Date endTime) {
        int page = Math.max(pageIndex, 1);
        int size = pageSize > 0 ? pageSize : 20;
        Specification<PatientFlowLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                preds.add(cb.or(
                        cb.like(root.get("patientCode"), like),
                        cb.like(root.get("patientName"), like),
                        cb.like(root.get("showNo"), like),
                        cb.like(root.get("operatorName"), like),
                        cb.like(root.get("operatorNo"), like)));
            }
            if (eventType != null && !eventType.isBlank()) {
                preds.add(cb.equal(root.get("eventType"), eventType.trim().toUpperCase()));
            }
            if (patientCode != null && !patientCode.isBlank()) {
                preds.add(cb.equal(root.get("patientCode"), patientCode.trim()));
            }
            if (callListId != null) {
                preds.add(cb.equal(root.get("callListId"), callListId));
            }
            if (startTime != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (endTime != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };
        Page<PatientFlowLog> result = patientFlowLogRepository.findAll(
                spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> people = new HashMap<>();
        body.put("data", result.getContent().stream().map(row -> toRow(row, people)).toList());
        body.put("pageInfo", Map.of("totalCount", result.getTotalElements()));
        return body;
    }

    @Override
    public List<Map<String, Object>> timelineByCallListId(Integer callListId) {
        if (callListId == null) {
            return List.of();
        }
        Map<String, String> people = new HashMap<>();
        return patientFlowLogRepository.findByCallListIdOrderByCreateTimeAsc(callListId)
                .stream().map(row -> toRow(row, people)).toList();
    }

    @Override
    public List<Map<String, Object>> timelineByPatientCode(String patientCode, Date startTime, Date endTime) {
        if (patientCode == null || patientCode.isBlank()) {
            return List.of();
        }
        Specification<PatientFlowLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("patientCode"), patientCode.trim()));
            if (startTime != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (endTime != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }
            query.orderBy(cb.asc(root.get("createTime")));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Map<String, String> people = new HashMap<>();
        return patientFlowLogRepository.findAll(spec).stream().map(row -> toRow(row, people)).toList();
    }

    private Map<String, Object> toRow(PatientFlowLog log, Map<String, String> people) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", log.getId());
        m.put("callListId", log.getCallListId());
        m.put("patientCode", log.getPatientCode());
        m.put("patientName", log.getPatientName());
        m.put("showNo", log.getShowNo());
        m.put("deptId", log.getDeptId());
        m.put("eventType", log.getEventType());
        m.put("eventLabel", log.getEventLabel());
        m.put("operatorSource", log.getOperatorSource());
        m.put("operatorSourceLabel", PatientFlowDisplayText.sourceLabel(log.getOperatorSource()));
        m.put("operatorNo", log.getOperatorNo());
        m.put("operatorName", log.getOperatorName());
        m.put("operatorDisplay", operatorDisplay(log, people));
        m.put("remark", PatientFlowDisplayText.humanizeRemark(log.getRemark(), log.getEventType()));
        m.put("payload", log.getPayload());
        m.put("createTime", log.getCreateTime());
        return m;
    }

    /**
     * 操作人展示：医生/护士解析成「姓名（工号）」，设备保留「名称（终端号）」。
     * 与来源不同：来源只说明是哪一端，这里说明是谁或哪台设备。
     */
    private String operatorDisplay(PatientFlowLog row, Map<String, String> people) {
        String no = row.getOperatorNo() == null ? "" : row.getOperatorNo().trim();
        String name = row.getOperatorName() == null ? "" : row.getOperatorName().trim();
        if (!no.isEmpty()) {
            if (!people.containsKey(no)) {
                people.put(no, resolvePerson(no));
            }
            String person = people.get(no);
            if (person != null && !person.isBlank()) {
                return person;
            }
            if (!name.isEmpty() && !name.equals(no) && !isOpaqueId(no)) {
                return name + "（" + no + "）";
            }
            if (!name.isEmpty()) {
                return name;
            }
            return isOpaqueId(no) ? null : no;
        }
        return name.isEmpty() ? null : name;
    }

    /** 按 doctor.id 或 users.user_no 解析；都不是则返回 null，由调用方退回终端名称 */
    private String resolvePerson(String no) {
        Doctor doctor = doctorRepository.findById(no).orElse(null);
        if (doctor != null) {
            return joinNameNo(doctor.getName(), doctor.getWorkNo(), no);
        }
        Users user = userRepository.findById(no).orElse(null);
        if (user != null) {
            return joinNameNo(user.getUserName(), user.getUserNo(), no);
        }
        return null;
    }

    private static String joinNameNo(String name, String workNo, String fallbackNo) {
        String who = name != null && !name.isBlank() ? name.trim() : null;
        String no = workNo != null && !workNo.isBlank() ? workNo.trim() : null;
        if (who != null && no != null && !who.equals(no)) {
            return who + "（" + no + "）";
        }
        if (who != null) {
            return who;
        }
        if (no != null) {
            return no;
        }
        return fallbackNo;
    }

    private static boolean isOpaqueId(String no) {
        return no.matches("(?i)[0-9a-f]{32}") || no.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
