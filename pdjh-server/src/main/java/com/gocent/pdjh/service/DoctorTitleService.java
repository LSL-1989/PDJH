package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.DictDoctorTitle;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.repository.DictDoctorTitleRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 医生职称字典：维护名称，并在改名时同步 doctor.title。
 * 消费者：DoctorTitleController、建库种子 DoctorTitleSeedRunner。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorTitleService {

    /**
     * 建库时注入的默认职称（卫生系列医师职务，含本系统展示用的住院医师）。
     * 仅空表写入；之后删除的项不会在下次启动时补回。
     */
    public static final String[] DEFAULT_TITLES = {
            "主任医师",
            "副主任医师",
            "主治医师",
            "住院医师",
            "医师",
            "医士"
    };

    private final DictDoctorTitleRepository dictDoctorTitleRepository;
    private final DoctorRepository doctorRepository;

    public List<DictDoctorTitle> listAll() {
        return dictDoctorTitleRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    /** 空表时写入默认职称；已有数据则不动 */
    @Transactional
    public int seedIfEmpty() {
        if (dictDoctorTitleRepository.count() > 0) {
            return 0;
        }
        int order = 1;
        for (String name : DEFAULT_TITLES) {
            DictDoctorTitle row = new DictDoctorTitle();
            row.setName(name);
            row.setSortOrder(order++);
            row.setEnabled(1);
            dictDoctorTitleRepository.save(row);
        }
        log.info("dict_doctor_title 写入 {} 条默认职称", DEFAULT_TITLES.length);
        return DEFAULT_TITLES.length;
    }

    /**
     * 新增或修改。id 为空为新增。
     * 修改名称时，把仍使用旧名称的 doctor.title 一并改掉。
     */
    @Transactional
    public DictDoctorTitle save(DictDoctorTitle input) {
        if (input == null) {
            throw new IllegalArgumentException("职称不能为空");
        }
        String name = input.getName() == null ? "" : input.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("职称名称不能为空");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("职称名称不能超过 50 字");
        }
        dictDoctorTitleRepository.findByName(name).ifPresent(dup -> {
            if (input.getId() == null || !dup.getId().equals(input.getId())) {
                throw new IllegalArgumentException("职称名称已存在：" + name);
            }
        });

        if (input.getId() == null) {
            DictDoctorTitle created = new DictDoctorTitle();
            created.setName(name);
            created.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : nextSortOrder());
            created.setEnabled(normalizeEnabled(input.getEnabled()));
            return dictDoctorTitleRepository.save(created);
        }

        DictDoctorTitle existing = dictDoctorTitleRepository.findById(input.getId())
                .orElseThrow(() -> new IllegalArgumentException("职称不存在"));
        String oldName = existing.getName();
        existing.setName(name);
        if (input.getSortOrder() != null) {
            existing.setSortOrder(input.getSortOrder());
        }
        if (input.getEnabled() != null) {
            existing.setEnabled(normalizeEnabled(input.getEnabled()));
        }
        DictDoctorTitle saved = dictDoctorTitleRepository.save(existing);
        if (oldName != null && !oldName.equals(name)) {
            List<Doctor> doctors = doctorRepository.findByTitle(oldName);
            for (Doctor doctor : doctors) {
                doctor.setTitle(name);
            }
            if (!doctors.isEmpty()) {
                doctorRepository.saveAll(doctors);
                log.info("职称「{}」更名为「{}」，已同步 {} 名医生", oldName, name, doctors.size());
            }
        }
        return saved;
    }

    /** 删除。仍被医生档案引用时拒绝 */
    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        DictDoctorTitle existing = dictDoctorTitleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("职称不存在"));
        long used = doctorRepository.countByTitle(existing.getName());
        if (used > 0) {
            throw new IllegalArgumentException("已有 " + used + " 名医生使用「" + existing.getName() + "」，请先调整档案后再删除");
        }
        dictDoctorTitleRepository.delete(existing);
    }

    private int nextSortOrder() {
        return dictDoctorTitleRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(DictDoctorTitle::getSortOrder)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private static int normalizeEnabled(Integer enabled) {
        return enabled != null && enabled == 0 ? 0 : 1;
    }
}
