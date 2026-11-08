package com.gocent.pdjh.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.constants.TriageStateConstants;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.NurseActionLog;
import com.gocent.pdjh.entity.NurseSchedule;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.NurseActionLogRepository;
import com.gocent.pdjh.repository.NurseScheduleRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.NurseDeskService;
import com.gocent.pdjh.service.PatientFlowLogService;
import com.gocent.pdjh.service.TriageAdminService;
import com.gocent.pdjh.service.TriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 护士工作站业务：诊区解析、告警聚合、签到兜底、队列干预与审计
 * 消费者：NurseDeskController、HJ护士工作站
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NurseDeskServiceImpl implements NurseDeskService {

    private static final String EMPTY_GUID = "00000000-0000-0000-0000-000000000000";
    private static final long OVERTIME_MS = 30L * 60 * 1000;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final NurseScheduleRepository nurseScheduleRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final CallListRepository callListRepository;
    private final NurseActionLogRepository nurseActionLogRepository;
    private final TriageAdminService triageAdminService;
    private final TriageService triageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final PatientFlowLogService patientFlowLogService;

    private record ZoneContext(String zoneId, String zoneName, List<Department> departments, List<String> deptIds) {}

    private record ZonePick(String zoneId, String source) {}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getSession(String operatorUserNo, String token, String requestedZoneId) {
        Users user = validateOperator(operatorUserNo, token);
        ZonePick pick = pickSessionZone(user, requestedZoneId);
        if ("LOGIN".equals(pick.source())) {
            upsertTodaySchedule(user.getUserNo(), pick.zoneId(), "FULL");
        }
        return toSessionMap(user, buildZoneContext(pick.zoneId()), pick);
    }

    @Override
    public Map<String, Object> listDutyZones(String operatorUserNo, String token) {
        Users user = validateOperator(operatorUserNo, token);
        return toDutyZoneMap(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> switchZone(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        String requested = stringVal(body != null ? body.get("zoneId") : null);
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException("zoneId 不能为空");
        }
        String canonical = toZoneId(requested);
        if (!authorizedZoneIds(user).contains(canonical)) {
            throw new IllegalArgumentException("该诊区不在授权范围内");
        }
        String previous = pickSessionZone(user, null).zoneId();
        upsertTodaySchedule(user.getUserNo(), canonical, "FULL");
        ZoneContext zone = buildZoneContext(canonical);
        Map<String, Object> auditBody = body != null ? new LinkedHashMap<>(body) : new LinkedHashMap<>();
        auditBody.put("fromZoneId", previous);
        auditBody.put("toZoneId", canonical);
        writeAudit(user, zone, "SWITCH_ZONE", null, canonical, auditBody);
        return toSessionMap(user, zone, new ZonePick(canonical, "SCHEDULE"));
    }

    @Override
    public Map<String, Object> getOverview(String operatorUserNo, String token) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        List<CallList> today = callListRepository.findTodayByDeptIdIn(zone.deptIds());
        List<Map<String, Object>> alerts = buildAlerts(zone, today);

        long waiting = today.stream().filter(this::isActiveWaiting).count();
        long unarrived = today.stream()
                .filter(c -> TriageStateConstants.UNARRIVED.equals(c.getTriageState())).count();
        long passed = today.stream()
                .filter(c -> TriageStateConstants.PASSED.equals(c.getTriageState())).count();
        long openRooms = officeRoomRepository.findAll().stream()
                .filter(r -> r.getDeptId() != null && zone.deptIds().contains(r.getDeptId()))
                .filter(r -> Integer.valueOf(1).equals(r.getState()))
                .count();
        long totalRooms = officeRoomRepository.findAll().stream()
                .filter(r -> r.getDeptId() != null && zone.deptIds().contains(r.getDeptId()))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("waitingCount", waiting);
        data.put("unarrivedCount", unarrived);
        data.put("passedCount", passed);
        data.put("alertCount", alerts.size());
        data.put("openRoomCount", openRooms);
        data.put("totalRoomCount", totalRooms);
        return data;
    }

    @Override
    public List<Map<String, Object>> getAlerts(String operatorUserNo, String token) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        List<CallList> today = callListRepository.findTodayByDeptIdIn(zone.deptIds());
        return buildAlerts(zone, today);
    }

    @Override
    public List<Map<String, Object>> searchPatient(String operatorUserNo, String token, String keyword) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return callListRepository.searchTodayInDepts(zone.deptIds(), keyword.trim()).stream()
                .map(this::toPatientMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualSignIn(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        requireReason(body);
        CallList call = requireCallInZone(zone, callListId);

        boolean skipApptCheck = toBoolean(body.get("skipApptCheck"));
        boolean skipSecondScan = toBoolean(body.get("skipSecondScan"));

        if (!skipApptCheck && Integer.valueOf(1).equals(call.getIsAppointment())) {
            assertWithinAppointmentWindow(call);
        }

        if (TriageStateConstants.UNARRIVED.equals(call.getTriageState()) || skipSecondScan) {
            call.setTriageState(TriageStateConstants.WAITING);
            call.setCheckInTime(new Date());
            if (call.getState() == null || call.getState() != 0) {
                call.setState(0);
            }
            callListRepository.save(call);
        } else {
            triageService.checkIn(callListId);
        }

        writeAudit(user, zone, "MANUAL_SIGN_IN", callListId, call.getDeptID(), body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindOfficeRoom(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        String officeRoomId = stringVal(body.get("officeRoomId"));
        requireReason(body);
        if (officeRoomId == null || officeRoomId.isBlank()) {
            throw new IllegalArgumentException("officeRoomId 不能为空");
        }
        CallList call = requireCallInZone(zone, callListId);
        OfficeRoom room = officeRoomRepository.findById(officeRoomId)
                .orElseThrow(() -> new IllegalArgumentException("诊室不存在"));
        if (!zone.deptIds().contains(room.getDeptId())) {
            throw new IllegalArgumentException("诊室不在当前诊区");
        }
        call.setOfficeRoomId(officeRoomId);
        callListRepository.save(call);
        writeAudit(user, zone, "BIND_OFFICE_ROOM", callListId, call.getDeptID(), body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revive(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        requireReason(body);
        requireCallInZone(zone, callListId);
        if (!triageAdminService.revive(callListId)) {
            throw new IllegalArgumentException("复活失败：患者不在过号状态或记录不存在");
        }
        writeAudit(user, zone, "REVIVE", callListId, null, body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCutIn(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        Integer targetXh = toInteger(body.get("targetXh"));
        if (targetXh == null) {
            targetXh = 0;
        }
        requireReason(body);
        requireCallInZone(zone, callListId);
        if (!triageAdminService.manualCutIn(callListId, targetXh)) {
            throw new IllegalArgumentException("插队失败");
        }
        writeAudit(user, zone, "MANUAL_CUT_IN", callListId, null, body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceSwap(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        String newDeptId = stringVal(body.get("newDeptId"));
        requireReason(body);
        if (newDeptId == null || newDeptId.isBlank()) {
            throw new IllegalArgumentException("newDeptId 不能为空");
        }
        if (!zone.deptIds().contains(newDeptId)) {
            throw new IllegalArgumentException("目标科室不在当前诊区");
        }
        requireCallInZone(zone, callListId);
        String transferType = stringVal(body.get("transferType"));
        String action = "CONSULT".equalsIgnoreCase(transferType) ? "FORCE_SWAP_CONSULT" : "FORCE_SWAP";
        if (!triageAdminService.forceSwap(callListId, newDeptId, stringVal(body.get("newDoctorId")))) {
            throw new IllegalArgumentException("转诊失败");
        }
        writeAudit(user, zone, action, callListId, newDeptId, body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setFrozen(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        requireReason(body);
        CallList call = requireCallInZone(zone, callListId);
        boolean frozen = toBoolean(body.get("frozen"));
        call.setIsForzen(frozen ? 1 : 0);
        callListRepository.save(call);
        writeAudit(user, zone, frozen ? "SET_FROZEN" : "UNSET_FROZEN", callListId, call.getDeptID(), body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markHisRefunded(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        Integer callListId = toInteger(body.get("callListId"));
        requireReason(body);
        CallList call = requireCallInZone(zone, callListId);
        call.setTriageState(TriageStateConstants.CANCELLED);
        call.setState(9);
        callListRepository.save(call);
        writeAudit(user, zone, "MARK_HIS_REFUNDED", callListId, call.getDeptID(), body);
        broadcastStateUpdate(callListId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendDept(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        String deptId = stringVal(body.get("deptId"));
        requireReason(body);
        Department dept = requireDeptInZone(zone, deptId);
        dept.setIsSuspended(1);
        departmentRepository.save(dept);
        writeAudit(user, zone, "SUSPEND_DEPT", null, deptId, body);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "DEPT_STATUS");
        msg.put("action", "SUSPENDED");
        msg.put("deptId", deptId);
        msg.put("message", (dept.getName() != null ? dept.getName() : deptId) + " 已停诊");
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
        broadcastStateUpdate(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeDept(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        String deptId = stringVal(body.get("deptId"));
        requireReason(body);
        Department dept = requireDeptInZone(zone, deptId);
        dept.setIsSuspended(0);
        departmentRepository.save(dept);
        writeAudit(user, zone, "RESUME_DEPT", null, deptId, body);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "DEPT_STATUS");
        msg.put("action", "RESUMED");
        msg.put("deptId", deptId);
        msg.put("message", (dept.getName() != null ? dept.getName() : deptId) + " 已恢复出诊");
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
        broadcastStateUpdate(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> migrateRoom(String operatorUserNo, String token, Map<String, Object> body) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        requireReason(body);
        String fromRoomId = stringVal(body.get("fromRoomId"));
        String toRoomId = stringVal(body.get("toRoomId"));
        if (fromRoomId == null || toRoomId == null) {
            throw new IllegalArgumentException("fromRoomId / toRoomId 不能为空");
        }
        if (fromRoomId.equals(toRoomId)) {
            throw new IllegalArgumentException("目标诊室不能与源诊室相同");
        }
        OfficeRoom from = officeRoomRepository.findById(fromRoomId)
                .orElseThrow(() -> new IllegalArgumentException("源诊室不存在"));
        OfficeRoom to = officeRoomRepository.findById(toRoomId)
                .orElseThrow(() -> new IllegalArgumentException("目标诊室不存在"));
        if (!zone.deptIds().contains(from.getDeptId()) || !zone.deptIds().contains(to.getDeptId())) {
            throw new IllegalArgumentException("诊室不在当前诊区");
        }
        if (to.getState() != null && to.getState() == 0) {
            throw new IllegalArgumentException("目标诊室已关闭");
        }

        List<CallList> waiting = callListRepository.findTodayCallListByOfficeRoomAndState(fromRoomId, 0);
        List<CallList> attending = callListRepository.findTodayCallListByOfficeRoomAndState(fromRoomId, 1);
        int moved = 0;
        for (CallList c : waiting) {
            c.setOfficeRoomId(toRoomId);
            c.setOutPatientOffice(to.getName());
            callListRepository.save(c);
            moved++;
        }
        for (CallList c : attending) {
            c.setOfficeRoomId(toRoomId);
            c.setOutPatientOffice(to.getName());
            callListRepository.save(c);
            moved++;
        }

        boolean moveDoctor = toBoolean(body.get("moveDoctor"));
        if (moveDoctor && from.getDoctorId() != null && !from.getDoctorId().isBlank()) {
            String docId = from.getDoctorId();
            from.setDoctorId(null);
            officeRoomRepository.save(from);
            to.setDoctorId(docId);
            officeRoomRepository.save(to);
        }

        writeAudit(user, zone, "MIGRATE_ROOM", null, from.getDeptId(), body);
        broadcastStateUpdate(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("movedCount", moved);
        result.put("fromRoomId", fromRoomId);
        result.put("toRoomId", toRoomId);
        return result;
    }

    @Override
    public Map<String, Object> getActionLogs(String operatorUserNo, String token, int page, int pageSize) {
        Users user = validateOperator(operatorUserNo, token);
        ZoneContext zone = resolveZone(user);
        int p = Math.max(page, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        Page<NurseActionLog> result = nurseActionLogRepository.findByZoneIdOrderByCreateTimeDesc(
                zone.zoneId(), PageRequest.of(p - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotalElements());
        data.put("page", p);
        data.put("pageSize", size);
        data.put("items", result.getContent());
        return data;
    }

    private Users validateOperator(String operatorUserNo, String token) {
        if (operatorUserNo == null || operatorUserNo.isBlank()) {
            throw new IllegalArgumentException("operatorUserNo 不能为空");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token 不能为空");
        }
        Users user = userRepository.findById(operatorUserNo)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.getToken() == null || !user.getToken().equals(token)) {
            throw new IllegalArgumentException("token 无效或已过期，请重新登录");
        }
        return user;
    }

    private ZoneContext resolveZone(Users user) {
        ZonePick pick = pickSessionZone(user, null);
        return buildZoneContext(pick.zoneId());
    }

    /**
     * 会话诊区优先级：当日 nurse_schedule → 登录传入的授权诊区 → users.dept_id 常驻。
     */
    private ZonePick pickSessionZone(Users user, String requestedZoneId) {
        NurseSchedule today = pickTodaySchedule(user.getUserNo());
        if (today != null && today.getDeptId() != null && !today.getDeptId().isBlank()) {
            return new ZonePick(toZoneId(today.getDeptId()), "SCHEDULE");
        }
        if (requestedZoneId != null && !requestedZoneId.isBlank()) {
            String canonical = toZoneId(requestedZoneId);
            if (!authorizedZoneIds(user).contains(canonical)) {
                throw new IllegalArgumentException("该诊区不在授权范围内");
            }
            return new ZonePick(canonical, "LOGIN");
        }
        String resident = residentZoneId(user);
        if (resident != null) {
            return new ZonePick(resident, "RESIDENT");
        }
        throw new IllegalArgumentException("用户未绑定诊区，请联系管理员配置常驻科室或今日排班");
    }

    private ZoneContext buildZoneContext(String zoneId) {
        Department zoneDept = departmentRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("诊区不存在: " + zoneId));
        LinkedHashSet<String> deptIds = new LinkedHashSet<>();
        deptIds.add(zoneId);
        collectDescendants(zoneId, deptIds);

        List<Department> departments = deptIds.stream()
                .map(departmentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(d -> d.getBusinessType() == null || "OUTPATIENT".equals(d.getBusinessType()))
                .collect(Collectors.toList());

        return new ZoneContext(zoneId, zoneDept.getName(), departments, new ArrayList<>(deptIds));
    }

    private Map<String, Object> toSessionMap(Users user, ZoneContext zone, ZonePick pick) {
        List<Map<String, Object>> deptList = zone.departments().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("isSuspended", d.getIsSuspended() != null ? d.getIsSuspended() : 0);
            return m;
        }).collect(Collectors.toList());

        Set<String> deptIdSet = new LinkedHashSet<>(zone.deptIds());
        List<Map<String, Object>> rooms = officeRoomRepository.findAll().stream()
                .filter(r -> r.getDeptId() != null && deptIdSet.contains(r.getDeptId()))
                .filter(r -> r.getBusinessType() == null || "OUTPATIENT".equals(r.getBusinessType()))
                .sorted(Comparator.comparing(OfficeRoom::getName, Comparator.nullsLast(String::compareTo)))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    m.put("deptId", r.getDeptId());
                    m.put("state", r.getState() != null ? r.getState() : 1);
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zoneId", zone.zoneId());
        data.put("zoneName", zone.zoneName());
        data.put("zoneSource", pick.source());
        data.put("fallbackHint", "RESIDENT".equals(pick.source()));
        data.put("departments", deptList);
        data.put("rooms", rooms);
        data.put("operatorName", user.getUserName());
        data.put("operatorRole", user.getRole());
        data.put("authorizedZones", authorizedZoneList(user));
        String resident = residentZoneId(user);
        data.put("residentZoneId", resident);
        data.put("residentZoneName", zoneNameOf(resident));
        return data;
    }

    private Map<String, Object> toDutyZoneMap(Users user) {
        Map<String, Object> data = new LinkedHashMap<>();
        NurseSchedule today = pickTodaySchedule(user.getUserNo());
        if (today != null && today.getDeptId() != null && !today.getDeptId().isBlank()) {
            String zoneId = toZoneId(today.getDeptId());
            Map<String, Object> sched = new LinkedHashMap<>();
            sched.put("zoneId", zoneId);
            sched.put("zoneName", zoneNameOf(zoneId));
            sched.put("shiftType", today.getShiftType());
            data.put("todaySchedule", sched);
        } else {
            data.put("todaySchedule", null);
        }
        String resident = residentZoneId(user);
        List<Map<String, Object>> zones = authorizedZoneList(user);
        data.put("residentZoneId", resident);
        data.put("residentZoneName", zoneNameOf(resident));
        data.put("authorizedZones", zones);
        data.put("fallbackHint", data.get("todaySchedule") == null);
        data.put("needPick", data.get("todaySchedule") == null && zones.size() > 1);
        return data;
    }

    private List<Map<String, Object>> authorizedZoneList(Users user) {
        List<Map<String, Object>> list = new ArrayList<>();
        String resident = residentZoneId(user);
        for (String id : authorizedZoneIds(user)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", zoneNameOf(id));
            if (id.equals(resident)) {
                m.put("source", "RESIDENT");
            } else {
                m.put("source", "SIBLING");
            }
            list.add(m);
        }
        return list;
    }

    /**
     * v1 授权集合（未接 data_scope）：常驻诊区 + 同父兄弟诊区 + 历史排班诊区。
     * 不能任选全院叶子科室，只能选诊区节点。
     */
    private LinkedHashSet<String> authorizedZoneIds(Users user) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        String resident = residentZoneId(user);
        if (resident != null) {
            ids.add(resident);
            departmentRepository.findById(resident).ifPresent(zoneDept -> {
                List<Department> siblings = hasParent(zoneDept.getFatherID())
                        ? departmentRepository.findByFatherID(zoneDept.getFatherID())
                        : departmentRepository.findByFatherID(EMPTY_GUID);
                for (Department sib : siblings) {
                    if (isZoneNode(sib)) {
                        ids.add(sib.getId());
                    }
                }
            });
        }
        for (NurseSchedule row : nurseScheduleRepository.findByUserNoOrderByScheduleDateDesc(user.getUserNo())) {
            if (row.getDeptId() == null || row.getDeptId().isBlank()) {
                continue;
            }
            try {
                ids.add(toZoneId(row.getDeptId()));
            } catch (IllegalArgumentException ignored) {
                // 历史排班指向已删科室时跳过
            }
        }
        return ids;
    }

    private String residentZoneId(Users user) {
        String deptId = user.getDeptID();
        if (deptId == null || deptId.isBlank()) {
            return null;
        }
        try {
            return toZoneId(deptId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 叶子科室归到父诊区；本身已是诊区（有子节点或 Type=1）则用自身。 */
    private String toZoneId(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            throw new IllegalArgumentException("诊区不能为空");
        }
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new IllegalArgumentException("科室/诊区不存在: " + deptId));
        boolean hasChildren = !departmentRepository.findByFatherID(dept.getId()).isEmpty();
        boolean branch = Integer.valueOf(1).equals(dept.getType());
        if (hasChildren || branch) {
            return dept.getId();
        }
        if (hasParent(dept.getFatherID())) {
            return dept.getFatherID();
        }
        return dept.getId();
    }

    private boolean isZoneNode(Department d) {
        if (d == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(d.getType())) {
            return true;
        }
        return !departmentRepository.findByFatherID(d.getId()).isEmpty();
    }

    private String zoneNameOf(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return null;
        }
        return departmentRepository.findById(zoneId).map(Department::getName).orElse(zoneId);
    }

    private NurseSchedule pickTodaySchedule(String userNo) {
        List<NurseSchedule> rows = nurseScheduleRepository.findByUserNoAndScheduleDateOrderByIdAsc(
                userNo, LocalDate.now());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        boolean afternoon = LocalTime.now().getHour() >= 12;
        NurseSchedule full = null;
        NurseSchedule am = null;
        NurseSchedule pm = null;
        for (NurseSchedule r : rows) {
            String st = r.getShiftType() == null ? "FULL" : r.getShiftType().toUpperCase();
            switch (st) {
                case "FULL" -> full = r;
                case "AM" -> am = r;
                case "PM" -> pm = r;
                default -> {
                }
            }
        }
        if (afternoon) {
            if (pm != null) {
                return pm;
            }
            if (full != null) {
                return full;
            }
            return am != null ? am : rows.get(0);
        }
        if (am != null) {
            return am;
        }
        if (full != null) {
            return full;
        }
        return pm != null ? pm : rows.get(0);
    }

    private void upsertTodaySchedule(String userNo, String zoneId, String shiftType) {
        LocalDate today = LocalDate.now();
        nurseScheduleRepository.deleteByUserNoAndScheduleDate(userNo, today);
        NurseSchedule row = new NurseSchedule();
        row.setUserNo(userNo);
        row.setDeptId(zoneId);
        row.setScheduleDate(today);
        row.setShiftType(shiftType);
        nurseScheduleRepository.save(row);
    }

    private void collectDescendants(String parentId, Set<String> ids) {
        for (Department ch : departmentRepository.findByFatherID(parentId)) {
            if (ids.add(ch.getId())) {
                collectDescendants(ch.getId(), ids);
            }
        }
    }

    private boolean hasParent(String fatherId) {
        return fatherId != null && !fatherId.isBlank() && !EMPTY_GUID.equalsIgnoreCase(fatherId);
    }

    private CallList requireCallInZone(ZoneContext zone, Integer callListId) {
        if (callListId == null) {
            throw new IllegalArgumentException("callListId 不能为空");
        }
        CallList call = callListRepository.findById(callListId)
                .orElseThrow(() -> new IllegalArgumentException("排队记录不存在"));
        if (call.getDeptID() == null || !zone.deptIds().contains(call.getDeptID())) {
            throw new IllegalArgumentException("该患者不在当前诊区");
        }
        return call;
    }

    private Department requireDeptInZone(ZoneContext zone, String deptId) {
        if (deptId == null || deptId.isBlank()) {
            throw new IllegalArgumentException("deptId 不能为空");
        }
        if (!zone.deptIds().contains(deptId)) {
            throw new IllegalArgumentException("科室不在当前诊区");
        }
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new IllegalArgumentException("科室不存在"));
    }

    private void requireReason(Map<String, Object> body) {
        String reasonCode = stringVal(body.get("reasonCode"));
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("请选择操作原因");
        }
        if ("OTHER".equals(reasonCode)) {
            String remark = stringVal(body.get("remark"));
            if (remark == null || remark.isBlank()) {
                throw new IllegalArgumentException("选择「其他」时必须填写备注");
            }
        }
    }

    private void assertWithinAppointmentWindow(CallList call) {
        String start = call.getAppointmentStartTime();
        String end = call.getAppointmentEndTime();
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            return;
        }
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime now = LocalTime.now();
            LocalTime s = LocalTime.parse(start.trim(), fmt);
            LocalTime e = LocalTime.parse(end.trim(), fmt);
            if (now.isBefore(s) || now.isAfter(e)) {
                throw new IllegalArgumentException("当前不在预约时段内，请使用 Override 签到");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("预约时段解析失败: {}-{}", start, end);
        }
    }

    private List<Map<String, Object>> buildAlerts(ZoneContext zone, List<CallList> today) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Department d : zone.departments()) {
            if (Integer.valueOf(1).equals(d.getIsSuspended())) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("alertType", "DEPT_SUSPENDED");
                a.put("severity", 3);
                a.put("deptId", d.getId());
                a.put("message", (d.getName() != null ? d.getName() : d.getId()) + " 已停诊");
                alerts.add(a);
            }
        }

        for (CallList c : today) {
            if (TriageStateConstants.CANCELLED.equals(c.getTriageState())) {
                continue;
            }
            if (TriageStateConstants.PASSED.equals(c.getTriageState())) {
                alerts.add(alertFromCall(c, "PASSED", 2, "过号待处置"));
                continue;
            }
            if (TriageStateConstants.UNARRIVED.equals(c.getTriageState())) {
                alerts.add(alertFromCall(c, "UNARRIVED", 2, "未签到"));
                continue;
            }
            if (isActiveWaiting(c) && (c.getOfficeRoomId() == null || c.getOfficeRoomId().isBlank())) {
                alerts.add(alertFromCall(c, "MISSING_ROOM", 2, "候诊中但未绑定诊室"));
            }
            if (isActiveWaiting(c) && isOvertime(c, now)) {
                alerts.add(alertFromCall(c, "OVERTIME", 3, "等候超时"));
            }
        }

        alerts.sort(Comparator
                .comparingInt((Map<String, Object> a) -> (Integer) a.getOrDefault("severity", 0)).reversed()
                .thenComparing(a -> stringVal(a.get("showNo")), Comparator.nullsLast(String::compareTo)));
        return alerts;
    }

    private Map<String, Object> alertFromCall(CallList c, String type, int severity, String message) {
        Map<String, Object> a = toPatientMap(c);
        a.put("alertType", type);
        a.put("severity", severity);
        a.put("callListId", c.getId());
        a.put("message", message + " · " + nullSafe(c.getShowNo()) + " " + nullSafe(c.getPatientName()));
        return a;
    }

    private Map<String, Object> toPatientMap(CallList c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("callListId", c.getId());
        m.put("showNo", c.getShowNo());
        m.put("patientName", c.getPatientName());
        m.put("patientCode", c.getPatientCode());
        m.put("triageState", c.getTriageState());
        m.put("state", c.getState());
        m.put("deptID", c.getDeptID());
        m.put("officeRoomId", c.getOfficeRoomId());
        m.put("isAppointment", c.getIsAppointment());
        m.put("appointmentStartTime", c.getAppointmentStartTime());
        m.put("appointmentEndTime", c.getAppointmentEndTime());
        return m;
    }

    private boolean isActiveWaiting(CallList c) {
        if (c.getState() == null || c.getState() != 0) {
            return false;
        }
        if (Integer.valueOf(1).equals(c.getIsForzen())) {
            return false;
        }
        String ts = c.getTriageState();
        return ts == null || TriageStateConstants.WAITING.equals(ts) || TriageStateConstants.REVISIT.equals(ts);
    }

    private boolean isOvertime(CallList c, long now) {
        Date ref = c.getCheckInTime() != null ? c.getCheckInTime() : c.getCallTime();
        if (ref == null) {
            return false;
        }
        return now - ref.getTime() > OVERTIME_MS;
    }

    private void writeAudit(Users user, ZoneContext zone, String actionType,
                            Integer callListId, String deptId, Map<String, Object> body) {
        NurseActionLog logEntry = new NurseActionLog();
        logEntry.setZoneId(zone.zoneId());
        logEntry.setCallListId(callListId);
        logEntry.setDeptId(deptId);
        logEntry.setOperatorUserNo(user.getUserNo());
        logEntry.setOperatorName(user.getUserName());
        logEntry.setOperatorRole(user.getRole());
        logEntry.setActionType(actionType);
        logEntry.setReasonCode(stringVal(body.get("reasonCode")));
        logEntry.setRemark(stringVal(body.get("remark")));
        try {
            logEntry.setPayload(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logEntry.setPayload(body.toString());
        }
        nurseActionLogRepository.save(logEntry);

        // 同步写入患者全流程留痕（科室级操作无 callListId 时跳过）
        PatientFlowEvent flowEvent = mapNurseActionToFlow(actionType);
        if (flowEvent != null && callListId != null) {
            CallList call = callListRepository.findById(callListId).orElse(null);
            if (call != null) {
                patientFlowLogService.record(call, flowEvent, "NURSE",
                        user.getUserNo(), user.getUserName(),
                        stringVal(body.get("remark")), body);
            } else {
                patientFlowLogService.recordRaw(callListId, null, null, null, deptId, flowEvent,
                        "NURSE", user.getUserNo(), user.getUserName(),
                        stringVal(body.get("remark")), body);
            }
        }
    }

    private static PatientFlowEvent mapNurseActionToFlow(String actionType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case "MANUAL_SIGN_IN" -> PatientFlowEvent.NURSE_MANUAL_SIGN_IN;
            case "BIND_OFFICE_ROOM" -> PatientFlowEvent.NURSE_BIND_ROOM;
            case "REVIVE" -> PatientFlowEvent.NURSE_REVIVE;
            case "MANUAL_CUT_IN" -> PatientFlowEvent.NURSE_CUT_IN;
            case "FORCE_SWAP", "FORCE_SWAP_CONSULT" -> PatientFlowEvent.NURSE_SWAP;
            case "SET_FROZEN" -> PatientFlowEvent.NURSE_FREEZE;
            case "UNSET_FROZEN" -> PatientFlowEvent.NURSE_UNFREEZE;
            case "MARK_HIS_REFUNDED" -> PatientFlowEvent.NURSE_REFUND;
            default -> null;
        };
    }

    private void broadcastStateUpdate(Integer callListId) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "STATE_UPDATE");
        if (callListId != null) {
            msg.put("callListId", callListId);
        }
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
    }

    private static String stringVal(Object v) {
        return v == null ? null : Objects.toString(v, null);
    }

    private static Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean toBoolean(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v == null) {
            return false;
        }
        return "true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString());
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
