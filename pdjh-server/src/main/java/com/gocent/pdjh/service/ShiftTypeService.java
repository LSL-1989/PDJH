package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.DictShiftType;
import com.gocent.pdjh.entity.DoctorSchedule;
import com.gocent.pdjh.repository.DictShiftTypeRepository;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 医生出诊班次字典：维护编码与时段，供排班页多选。
 * 消费者：ShiftTypeController、出诊排班保存校验、叫号器登录按当前时段选诊室。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftTypeService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 空表时写入的默认班次。只在没有任何记录时写入，已删除的项不会在下次启动补回。
     * 编码与历史排班里的 AM/PM/FULL/OFF 对齐。
     */
    private static final DefaultShift[] DEFAULTS = {
            new DefaultShift("AM", "上午班", "08:00", "12:00", "am", 1, 0),
            new DefaultShift("PM", "下午班", "13:30", "17:30", "pm", 2, 0),
            new DefaultShift("FULL", "全天班", "08:00", "17:30", "full", 3, 0),
            new DefaultShift("OFF", "停诊/休假", null, null, "off", 4, 1)
    };

    private final DictShiftTypeRepository dictShiftTypeRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public List<DictShiftType> listAll() {
        return dictShiftTypeRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public int seedIfEmpty() {
        if (dictShiftTypeRepository.count() > 0) {
            return 0;
        }
        for (DefaultShift item : DEFAULTS) {
            dictShiftTypeRepository.save(toEntity(item));
        }
        log.info("dict_shift_type 写入 {} 条默认班次", DEFAULTS.length);
        return DEFAULTS.length;
    }

    /**
     * 新增或修改。id 为空为新增。编码创建后不可改。
     * 入参 code/name/startTime/endTime/tagStyle/sortOrder/enabled/exclusiveFlag。
     */
    @Transactional
    public DictShiftType save(DictShiftType input) {
        if (input == null) {
            throw new IllegalArgumentException("班次不能为空");
        }
        String code = normalizeCode(input.getCode());
        String name = input.getName() == null ? "" : input.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("班次名称不能为空");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("班次名称不能超过 50 字");
        }
        int exclusive = normalizeFlag(input.getExclusiveFlag());
        String start = blankToNull(input.getStartTime());
        String end = blankToNull(input.getEndTime());
        validateTimes(exclusive, start, end);
        String tagStyle = normalizeTagStyle(input.getTagStyle(), exclusive);

        dictShiftTypeRepository.findByName(name).ifPresent(dup -> {
            if (input.getId() == null || !dup.getId().equals(input.getId())) {
                throw new IllegalArgumentException("班次名称已存在：" + name);
            }
        });
        dictShiftTypeRepository.findByCode(code).ifPresent(dup -> {
            if (input.getId() == null || !dup.getId().equals(input.getId())) {
                throw new IllegalArgumentException("班次编码已存在：" + code);
            }
        });

        if (input.getId() == null) {
            DictShiftType created = new DictShiftType();
            created.setCode(code);
            created.setName(name);
            created.setStartTime(start);
            created.setEndTime(end);
            created.setTagStyle(tagStyle);
            created.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : nextSortOrder());
            created.setEnabled(normalizeEnabled(input.getEnabled()));
            created.setExclusiveFlag(exclusive);
            return dictShiftTypeRepository.save(created);
        }

        DictShiftType existing = dictShiftTypeRepository.findById(input.getId())
                .orElseThrow(() -> new IllegalArgumentException("班次不存在"));
        if (!existing.getCode().equals(code)) {
            throw new IllegalArgumentException("班次编码创建后不可修改");
        }
        existing.setName(name);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setTagStyle(tagStyle);
        if (input.getSortOrder() != null) {
            existing.setSortOrder(input.getSortOrder());
        }
        if (input.getEnabled() != null) {
            existing.setEnabled(normalizeEnabled(input.getEnabled()));
        }
        existing.setExclusiveFlag(exclusive);
        return dictShiftTypeRepository.save(existing);
    }

    /** 删除。仍被出诊排班引用时拒绝 */
    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        DictShiftType existing = dictShiftTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("班次不存在"));
        if (doctorScheduleRepository.existsByShiftType(existing.getCode())) {
            throw new IllegalArgumentException("已有排班使用「" + existing.getName() + "」，请先清除相关排班或改为停用");
        }
        dictShiftTypeRepository.delete(existing);
    }

    /** 排班保存前校验编码存在；返回规范大写编码 */
    public DictShiftType requireKnown(String code) {
        String normalized = normalizeCode(code);
        return dictShiftTypeRepository.findByCode(normalized)
                .orElseThrow(() -> new IllegalArgumentException("班次不存在：" + normalized + "，请先在系统管理维护排班类型"));
    }

    /** 停诊类，或历史编码 OFF（即使字典被停用仍视为停诊） */
    public boolean isStop(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if ("OFF".equalsIgnoreCase(code.trim())) {
            return true;
        }
        return dictShiftTypeRepository.findByCode(code.trim().toUpperCase())
                .map(item -> item.getExclusiveFlag() != null && item.getExclusiveFlag() == 1)
                .orElse(false);
    }

    /**
     * 同一天多条排班时，按当前时刻选出诊室。
     * 优先命中时段覆盖 now 的非停诊班次；否则取第一条带诊室的非停诊班次。
     */
    public Optional<String> pickOfficeRoomId(List<DoctorSchedule> schedules, LocalTime now) {
        if (schedules == null || schedules.isEmpty()) {
            return Optional.empty();
        }
        List<DoctorSchedule> working = schedules.stream()
                .filter(item -> item.getOfficeRoomId() != null && !item.getOfficeRoomId().isBlank())
                .filter(item -> !isStop(item.getShiftType()))
                .filter(item -> item.getStopFlag() == null || item.getStopFlag() != 1)
                .toList();
        if (working.isEmpty()) {
            return Optional.empty();
        }
        if (now != null) {
            for (DoctorSchedule item : working) {
                DictShiftType type = dictShiftTypeRepository.findByCode(
                        item.getShiftType() == null ? "" : item.getShiftType().trim().toUpperCase()).orElse(null);
                if (type != null && covers(type, now)) {
                    return Optional.of(item.getOfficeRoomId());
                }
            }
        }
        return Optional.of(working.get(0).getOfficeRoomId());
    }

    private boolean covers(DictShiftType type, LocalTime now) {
        LocalTime start = parseTime(type.getStartTime());
        LocalTime end = parseTime(type.getEndTime());
        if (start == null || end == null) {
            return false;
        }
        if (end.isAfter(start)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), HM);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void validateTimes(int exclusive, String start, String end) {
        if (start == null && end == null) {
            if (exclusive == 1) {
                return;
            }
            throw new IllegalArgumentException("非停诊班次请填写开始和结束时间");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("开始时间和结束时间需同时填写");
        }
        if (parseTime(start) == null || parseTime(end) == null) {
            throw new IllegalArgumentException("时间格式应为 HH:mm");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("开始时间与结束时间不能相同");
        }
    }

    private String normalizeCode(String code) {
        String value = code == null ? "" : code.trim().toUpperCase();
        if (!value.matches("[A-Z][A-Z0-9_]{0,15}")) {
            throw new IllegalArgumentException("班次编码须为字母开头，仅含字母、数字、下划线，最长 16 位");
        }
        return value;
    }

    private static String normalizeTagStyle(String tagStyle, int exclusive) {
        String value = tagStyle == null ? "" : tagStyle.trim().toLowerCase();
        if (value.isEmpty()) {
            return exclusive == 1 ? "off" : "default";
        }
        if (!value.matches("am|pm|full|off|default")) {
            throw new IllegalArgumentException("展示样式无效");
        }
        return value;
    }

    private int nextSortOrder() {
        return dictShiftTypeRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(DictShiftType::getSortOrder)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private static int normalizeEnabled(Integer enabled) {
        return enabled != null && enabled == 0 ? 0 : 1;
    }

    private static int normalizeFlag(Integer flag) {
        return flag != null && flag == 1 ? 1 : 0;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static DictShiftType toEntity(DefaultShift item) {
        DictShiftType row = new DictShiftType();
        row.setCode(item.code);
        row.setName(item.name);
        row.setStartTime(item.startTime);
        row.setEndTime(item.endTime);
        row.setTagStyle(item.tagStyle);
        row.setSortOrder(item.sortOrder);
        row.setEnabled(1);
        row.setExclusiveFlag(item.exclusiveFlag);
        return row;
    }

    private record DefaultShift(String code, String name, String startTime, String endTime,
                                String tagStyle, int sortOrder, int exclusiveFlag) {
    }
}
