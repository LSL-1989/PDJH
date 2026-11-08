package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.DictShiftType;
import com.gocent.pdjh.entity.DoctorSchedule;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import com.gocent.pdjh.service.ShiftTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医生出诊排班接口。
 * 消费者：出诊排班甘特页、医生管理查看排班抽屉。
 * 同一医生同一天可有多条班次（诊室各自独立）；班次编码来自排班类型字典。
 */
@RestController
@RequestMapping("/DoctorSchedule")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DoctorScheduleRepository scheduleRepository;
    private final ShiftTypeService shiftTypeService;

    /**
     * 查询排班。
     * weekStart（周一 yyyy-MM-dd）查该周；startDate + endDate 查一段日期（日历月视图，含格子里跨月的日期）。
     * 只传 doctorId 时返回该医生全部排班（查看抽屉用，范围与周/月视图不同）。
     * deptId 仅在按日期范围查询时过滤。
     */
    @GetMapping("/list")
    public ResultData<List<DoctorSchedule>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String doctorId) {
        String doctor = blankToNull(doctorId);
        if (startDate != null || endDate != null) {
            if (startDate == null || endDate == null) {
                return ResultData.fail(400, "startDate 与 endDate 需同时传入");
            }
            if (endDate.isBefore(startDate)) {
                return ResultData.fail(400, "endDate 不能早于 startDate");
            }
            return ResultData.success(queryRange(startDate, endDate, deptId, doctor), "获取成功");
        }
        if (weekStart == null && doctor == null) {
            return ResultData.fail(400, "weekStart、日期范围或 doctorId 不能为空");
        }
        if (weekStart == null) {
            List<DoctorSchedule> rows = new ArrayList<>(scheduleRepository.findByDoctorId(doctor));
            rows.sort(Comparator.comparing(DoctorSchedule::getScheduleDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(DoctorSchedule::getId, Comparator.nullsLast(Long::compareTo)));
            return ResultData.success(rows, "获取成功");
        }
        return ResultData.success(queryRange(weekStart, weekStart.plusDays(6), deptId, doctor), "获取成功");
    }

    private List<DoctorSchedule> queryRange(LocalDate start, LocalDate end, String deptId, String doctor) {
        String dept = blankToNull(deptId);
        List<DoctorSchedule> list = (dept != null)
                ? scheduleRepository.findByScheduleDateBetweenAndDeptIdOrderByDoctorIdAscScheduleDateAsc(start, end, dept)
                : scheduleRepository.findByScheduleDateBetweenOrderByDoctorIdAscScheduleDateAsc(start, end);
        if (doctor != null) {
            list = list.stream().filter(item -> doctor.equals(item.getDoctorId())).toList();
        }
        return list;
    }

    /** 曾经排过班的医生 id。下拉和列表用，不带周明细，因此不并进 /list */
    @GetMapping("/scheduledDoctorIds")
    public ResultData<List<String>> scheduledDoctorIds() {
        return ResultData.success(scheduleRepository.findDistinctDoctorIds(), "获取成功");
    }

    /**
     * 保存排班。过往日期拒绝。
     * shifts 数组：覆盖这些日期上该医生的全部班次（同一天可含多个班次，诊室各自填写）。
     * 仅 shiftType：写入这一班；停诊类会清掉当天其他班次，普通班次只替换同编码并去掉停诊。
     * 日期用 scheduleDate，或 scheduleDates 数组（一次写入多天，同一事务）。
     */
    @PostMapping("/save")
    @Transactional
    public ResultData<List<DoctorSchedule>> save(@RequestBody Map<String, Object> body) {
        String doctorId = str(body.get("doctorId"));
        if (doctorId == null || doctorId.isBlank()) {
            return ResultData.fail(400, "doctorId 不能为空");
        }
        List<LocalDate> dates;
        try {
            dates = parseDates(body);
        } catch (DateTimeParseException ex) {
            return ResultData.fail(400, "日期格式应为 yyyy-MM-dd");
        }
        if (dates.isEmpty()) {
            return ResultData.fail(400, "scheduleDate 不能为空");
        }
        LocalDate today = LocalDate.now(ZONE);
        for (LocalDate date : dates) {
            if (date.isBefore(today)) {
                return ResultData.fail(400, "过往日期不可排班");
            }
        }
        List<ShiftDraft> drafts;
        try {
            drafts = parseShifts(body);
        } catch (IllegalArgumentException ex) {
            return ResultData.fail(400, ex.getMessage());
        }
        if (drafts.isEmpty()) {
            return ResultData.fail(400, "请选择班次");
        }
        boolean replaceDay = body.containsKey("shifts");
        try {
            List<DoctorSchedule> saved = new ArrayList<>();
            for (LocalDate date : dates) {
                saved.addAll(applyDay(doctorId, str(body.get("deptId")), date, drafts, replaceDay));
            }
            return ResultData.success(saved, "保存成功");
        } catch (IllegalArgumentException ex) {
            return ResultData.fail(400, ex.getMessage());
        }
    }

    /**
     * 删除排班。body：doctorId、scheduleDate 必填；shiftType 有值时只删该班次，否则删当天全部。
     */
    @PostMapping("/delete")
    @Transactional
    public ResultData<Boolean> delete(@RequestBody Map<String, Object> body) {
        String doctorId = str(body.get("doctorId"));
        String scheduleDateStr = str(body.get("scheduleDate"));
        if (doctorId == null || scheduleDateStr == null) {
            return ResultData.fail(400, "参数缺失");
        }
        LocalDate scheduleDate;
        try {
            scheduleDate = LocalDate.parse(scheduleDateStr);
        } catch (DateTimeParseException ex) {
            return ResultData.fail(400, "日期格式应为 yyyy-MM-dd");
        }
        String shiftType = str(body.get("shiftType"));
        List<DoctorSchedule> rows = scheduleRepository.findByDoctorIdAndScheduleDate(doctorId, scheduleDate);
        if (shiftType != null && !shiftType.isBlank()) {
            String code = shiftType.toUpperCase();
            rows = rows.stream().filter(item -> code.equalsIgnoreCase(item.getShiftType())).toList();
        }
        if (!rows.isEmpty()) {
            scheduleRepository.deleteAll(rows);
        }
        return ResultData.success(true, "删除成功");
    }

    /**
     * 按范围清除某医生排班。body：doctorId；scope=TODAY（当日）/ FROM_TODAY（当日及以后）/ ALL（全部，默认）。
     * 返回清除条数。
     */
    @PostMapping("/deleteByDoctor")
    @Transactional
    public ResultData<Integer> deleteByDoctor(@RequestBody Map<String, Object> body) {
        String doctorId = str(body.get("doctorId"));
        if (doctorId == null || doctorId.isBlank()) {
            return ResultData.fail(400, "doctorId 不能为空");
        }
        String scope = str(body.get("scope"));
        if (scope == null || scope.isBlank()) {
            scope = "ALL";
        }
        scope = scope.toUpperCase();
        LocalDate today = LocalDate.now(ZONE);
        List<DoctorSchedule> rows = scheduleRepository.findByDoctorId(doctorId);
        List<DoctorSchedule> targets = switch (scope) {
            case "TODAY" -> rows.stream().filter(item -> today.equals(item.getScheduleDate())).toList();
            case "FROM_TODAY" -> rows.stream()
                    .filter(item -> item.getScheduleDate() != null && !item.getScheduleDate().isBefore(today))
                    .toList();
            case "ALL" -> rows;
            default -> null;
        };
        if (targets == null) {
            return ResultData.fail(400, "scope 须为 TODAY、FROM_TODAY 或 ALL");
        }
        if (!targets.isEmpty()) {
            scheduleRepository.deleteAll(targets);
        }
        int count = targets.size();
        return ResultData.success(count, count > 0 ? "已清除 " + count + " 条排班" : "该医生暂无符合范围的排班");
    }

    private List<DoctorSchedule> applyDay(String doctorId, String deptId, LocalDate date,
                                           List<ShiftDraft> drafts, boolean replaceDay) {
        boolean hasStop = drafts.stream().anyMatch(item -> shiftTypeService.isStop(item.code));
        if (hasStop && drafts.size() > 1) {
            throw new IllegalArgumentException("停诊类班次不能与其他班次同时保存");
        }
        List<DoctorSchedule> existing = scheduleRepository.findByDoctorIdAndScheduleDate(doctorId, date);
        if (replaceDay || hasStop) {
            if (!existing.isEmpty()) {
                scheduleRepository.deleteAll(existing);
                scheduleRepository.flush();
            }
            List<DoctorSchedule> saved = new ArrayList<>();
            for (ShiftDraft draft : drafts) {
                saved.add(scheduleRepository.save(newRow(doctorId, deptId, date, draft)));
            }
            return saved;
        }
        List<DoctorSchedule> remove = existing.stream()
                .filter(item -> drafts.stream().anyMatch(draft -> draft.code.equalsIgnoreCase(item.getShiftType()))
                        || shiftTypeService.isStop(item.getShiftType()))
                .toList();
        if (!remove.isEmpty()) {
            scheduleRepository.deleteAll(remove);
            scheduleRepository.flush();
        }
        List<DoctorSchedule> saved = new ArrayList<>();
        for (ShiftDraft draft : drafts) {
            saved.add(scheduleRepository.save(newRow(doctorId, deptId, date, draft)));
        }
        return saved;
    }

    private DoctorSchedule newRow(String doctorId, String deptId, LocalDate date, ShiftDraft draft) {
        DoctorSchedule row = new DoctorSchedule();
        row.setDoctorId(doctorId);
        row.setDeptId(deptId);
        row.setScheduleDate(date);
        row.setShiftType(draft.code);
        row.setStopFlag(draft.stopFlag);
        row.setMaxQuota(draft.stopFlag != null && draft.stopFlag == 1 ? null : draft.maxQuota);
        row.setRemark(draft.remark);
        row.setOfficeRoomId(draft.stopFlag != null && draft.stopFlag == 1 ? null : draft.officeRoomId);
        return row;
    }

    private List<ShiftDraft> parseShifts(Map<String, Object> body) {
        List<Map<String, Object>> raw = new ArrayList<>();
        Object shifts = body.get("shifts");
        if (shifts instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    map.forEach((key, value) -> copy.put(String.valueOf(key), value));
                    raw.add(copy);
                }
            }
        } else {
            String shiftType = str(body.get("shiftType"));
            if (shiftType != null && !shiftType.isBlank()) {
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("shiftType", shiftType);
                one.put("maxQuota", body.get("maxQuota"));
                one.put("remark", body.get("remark"));
                one.put("officeRoomId", body.get("officeRoomId"));
                one.put("stopFlag", body.get("stopFlag"));
                raw.add(one);
            }
        }
        List<ShiftDraft> drafts = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            DictShiftType type = shiftTypeService.requireKnown(str(item.get("shiftType")));
            if (drafts.stream().anyMatch(draft -> draft.code.equals(type.getCode()))) {
                throw new IllegalArgumentException("班次重复：" + type.getName());
            }
            ShiftDraft draft = new ShiftDraft();
            draft.code = type.getCode();
            draft.remark = str(item.get("remark"));
            boolean stopped = shiftTypeService.isStop(draft.code) || flagOn(item.get("stopFlag"));
            draft.stopFlag = stopped ? 1 : 0;
            if (stopped) {
                draft.maxQuota = null;
                draft.officeRoomId = null;
            } else {
                draft.maxQuota = intOrNull(item.get("maxQuota"));
                String roomId = str(item.get("officeRoomId"));
                draft.officeRoomId = roomId == null || roomId.isBlank() ? null : roomId;
            }
            drafts.add(draft);
        }
        return drafts;
    }

    private static List<LocalDate> parseDates(Map<String, Object> body) {
        List<LocalDate> dates = new ArrayList<>();
        Object many = body.get("scheduleDates");
        if (many instanceof List<?> list) {
            for (Object item : list) {
                String value = str(item);
                if (value != null && !value.isBlank()) {
                    dates.add(LocalDate.parse(value));
                }
            }
        }
        if (dates.isEmpty()) {
            String one = str(body.get("scheduleDate"));
            if (one != null && !one.isBlank()) {
                dates.add(LocalDate.parse(one));
            }
        }
        return dates;
    }

    private static Integer intOrNull(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("号源限制必须是整数");
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString().trim();
    }

    /** 1 或 true 表示该午别停诊 */
    private static boolean flagOn(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        String text = value.toString().trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static final class ShiftDraft {
        private String code;
        private Integer maxQuota;
        private String remark;
        private String officeRoomId;
        private Integer stopFlag;
    }
}
