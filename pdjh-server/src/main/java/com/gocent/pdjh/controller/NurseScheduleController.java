package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.NurseSchedule;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.NurseScheduleRepository;
import com.gocent.pdjh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 护士当班排班维护；与医生出诊排班对称，不合并（对象是 users 而非 doctor）。
 * 消费者：HJ管理后台 nurseSchedule 页。
 */
@RestController
@RequestMapping("/NurseSchedule")
@RequiredArgsConstructor
public class NurseScheduleController {

    private static final Set<String> SHIFTS = Set.of("AM", "PM", "FULL");

    private final NurseScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * 列出排班。date 查单日（缺省今天）；startDate 与 endDate 同时传入则查闭区间，供周视图。
     * 可选 userNo 只在单日查询时生效。出参含 userName / zoneName。
     */
    @PostMapping("/List")
    public ResultData<List<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> body) {
        String startRaw = body != null ? str(body.get("startDate")) : null;
        String endRaw = body != null ? str(body.get("endDate")) : null;
        boolean hasStart = startRaw != null && !startRaw.isBlank();
        boolean hasEnd = endRaw != null && !endRaw.isBlank();
        List<NurseSchedule> rows;
        if (hasStart || hasEnd) {
            if (!hasStart || !hasEnd) {
                return ResultData.fail(400, "startDate 与 endDate 需同时传入");
            }
            LocalDate startDate = LocalDate.parse(startRaw);
            LocalDate endDate = LocalDate.parse(endRaw);
            if (endDate.isBefore(startDate)) {
                return ResultData.fail(400, "endDate 不能早于 startDate");
            }
            rows = scheduleRepository.findByScheduleDateBetweenOrderByScheduleDateAscUserNoAsc(startDate, endDate);
        } else {
            LocalDate date = parseDate(body != null ? str(body.get("date")) : null, LocalDate.now());
            String userNo = body != null ? str(body.get("userNo")) : null;
            rows = (userNo != null && !userNo.isBlank())
                    ? scheduleRepository.findByUserNoAndScheduleDateOrderByIdAsc(userNo, date)
                    : scheduleRepository.findByScheduleDateOrderByUserNoAsc(date);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (NurseSchedule row : rows) {
            out.add(toRow(row));
        }
        return ResultData.success(out, "获取成功");
    }

    /**
     * 保存护士当班。单条仍用 userNo + scheduleDate；周视图批量用 userNos + scheduleDates。
     * 每个护士每个日期先删后插，同一天只保留一个班次。过往日期拒绝。shiftType 默认 FULL。
     * 与医生排班不合并：对象是 users，且护士同一天不并存多个午别。
     */
    @PostMapping("/Save")
    @Transactional
    public ResultData<Object> save(@RequestBody Map<String, Object> body) {
        String deptId = str(body.get("deptId"));
        List<String> userNos = distinctStrings(body.get("userNos"));
        if (userNos.isEmpty()) {
            String one = str(body.get("userNo"));
            if (one != null && !one.isBlank()) {
                userNos.add(one);
            }
        }
        List<LocalDate> dates = parseDateList(body.get("scheduleDates"));
        if (dates.isEmpty()) {
            String one = str(body.get("scheduleDate"));
            if (one != null && !one.isBlank()) {
                dates.add(LocalDate.parse(one));
            }
        }
        dates = new ArrayList<>(new LinkedHashSet<>(dates));
        if (userNos.isEmpty() || deptId == null || deptId.isBlank() || dates.isEmpty()) {
            return ResultData.fail(400, "userNo / deptId / scheduleDate 不能为空");
        }
        if (!departmentRepository.existsById(deptId)) {
            return ResultData.fail(400, "科室/诊区不存在");
        }
        String shift = str(body.get("shiftType"));
        if (shift == null || shift.isBlank()) {
            shift = "FULL";
        }
        shift = shift.toUpperCase();
        if (!SHIFTS.contains(shift)) {
            return ResultData.fail(400, "shiftType 须为 AM / PM / FULL");
        }
        LocalDate today = LocalDate.now();
        for (String userNo : userNos) {
            Users user = userRepository.findById(userNo)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userNo));
            if (user.getLoginScene() != null && !user.getLoginScene().isBlank()
                    && !LoginScene.NURSE.getCode().equalsIgnoreCase(user.getLoginScene())) {
                return ResultData.fail(400, "仅可为护士站账号排班");
            }
        }
        List<Map<String, Object>> saved = new ArrayList<>();
        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                return ResultData.fail(400, "过往日期不可排班");
            }
            for (String userNo : userNos) {
                scheduleRepository.deleteByUserNoAndScheduleDate(userNo, date);
                NurseSchedule row = new NurseSchedule();
                row.setUserNo(userNo);
                row.setDeptId(deptId);
                row.setScheduleDate(date);
                row.setShiftType(shift);
                saved.add(toRow(scheduleRepository.save(row)));
            }
        }
        if (saved.size() == 1) {
            return ResultData.success(saved.get(0), "保存成功");
        }
        return ResultData.success(saved, "保存成功");
    }

    /** 按 id 删除一条排班；入参 id。 */
    @PostMapping("/Delete")
    public ResultData<Boolean> delete(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        if (id == null) {
            return ResultData.fail(400, "id 不能为空");
        }
        if (!scheduleRepository.existsById(id)) {
            return ResultData.fail(400, "排班不存在");
        }
        scheduleRepository.deleteById(id);
        return ResultData.success(true, "删除成功");
    }

    private Map<String, Object> toRow(NurseSchedule row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("userNo", row.getUserNo());
        m.put("deptId", row.getDeptId());
        m.put("scheduleDate", row.getScheduleDate() != null ? row.getScheduleDate().toString() : null);
        m.put("shiftType", row.getShiftType());
        m.put("createTime", row.getCreateTime());
        userRepository.findById(row.getUserNo()).ifPresent(u -> m.put("userName", u.getUserName()));
        departmentRepository.findById(row.getDeptId()).map(Department::getName)
                .ifPresent(name -> m.put("zoneName", name));
        if (!m.containsKey("userName")) {
            m.put("userName", row.getUserNo());
        }
        if (!m.containsKey("zoneName")) {
            m.put("zoneName", row.getDeptId());
        }
        return m;
    }

    private static LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(raw.trim());
    }

    private static String str(Object v) {
        return v == null ? null : v.toString().trim();
    }

    private static List<String> distinctStrings(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String text = str(item);
                if (text != null && !text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private static List<LocalDate> parseDateList(Object raw) {
        List<LocalDate> dates = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String text = str(item);
                if (text != null && !text.isBlank()) {
                    dates.add(LocalDate.parse(text));
                }
            }
        }
        return dates;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
