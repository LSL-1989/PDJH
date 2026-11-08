package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.config.DoctorAvatarResourceConfig;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.DoctorDept;
import com.gocent.pdjh.entity.PendingIssue;
import com.gocent.pdjh.repository.DoctorDeptRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import com.gocent.pdjh.service.DepartmentService;
import com.gocent.pdjh.service.DoctorService;
import com.gocent.pdjh.service.PendingIssueService;
import com.gocent.pdjh.service.StationUserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorDeptRepository doctorDeptRepository;
    private final DepartmentService departmentService;
    private final PendingIssueService pendingIssueService;
    private final StationUserSyncService stationUserSyncService;

    @Override
    public List<Doctor> listAll() {
        List<Doctor> doctors = doctorRepository.findAll();
        if (doctors.isEmpty()) {
            return doctors;
        }
        List<String> ids = doctors.stream().map(Doctor::getId).filter(Objects::nonNull).toList();
        Map<String, LinkedHashSet<String>> grouped = new HashMap<>();
        if (!ids.isEmpty()) {
            for (DoctorDept link : doctorDeptRepository.findByDoctorIdIn(ids)) {
                if (link.getDeptId() == null || link.getDeptId().isBlank()) {
                    continue;
                }
                grouped.computeIfAbsent(link.getDoctorId(), key -> new LinkedHashSet<>()).add(link.getDeptId());
            }
        }
        for (Doctor doctor : doctors) {
            LinkedHashSet<String> deptIds = new LinkedHashSet<>(grouped.getOrDefault(doctor.getId(), new LinkedHashSet<>()));
            if (doctor.getDeptId() != null && !doctor.getDeptId().isBlank()) {
                deptIds.add(doctor.getDeptId());
            }
            doctor.setDeptIds(new ArrayList<>(deptIds));
        }
        return doctors;
    }

    @Override
    public Map<String, Object> pageList(Map<String, Object> query) {
        int index = parseInt(query.get("Index"), 1);
        int pageSize = parseInt(query.get("Page"), 10);
        String name = extractStr(query, "name", "Intro2");
        String workNo = extractStr(query, "workNo", "ASID");
        PageRequest pr = PageRequest.of(
                Math.max(0, index - 1),
                Math.max(1, pageSize),
                Sort.by(Sort.Direction.DESC, "createDate"));
        Page<Doctor> pg;
        if (name.isEmpty() && workNo.isEmpty()) {
            pg = doctorRepository.findAll(pr);
        } else if (!name.isEmpty() && workNo.isEmpty()) {
            pg = doctorRepository.findByNameContainingIgnoreCase(name, pr);
        } else if (name.isEmpty()) {
            pg = doctorRepository.findByWorkNoContainingIgnoreCase(workNo, pr);
        } else {
            pg = doctorRepository.findByNameContainingIgnoreCaseAndWorkNoContainingIgnoreCase(name, workNo, pr);
        }
        List<Map<String, Object>> rows = pg.getContent().stream().map(this::toRow).collect(Collectors.toList());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("Data", rows);
        res.put("Totle", pg.getTotalElements());
        res.put("PageCount", pg.getTotalPages());
        res.put("Index", index);
        res.put("Page", pageSize);
        res.put("name", name);
        res.put("workNo", workNo);
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Doctor saveFromMap(Map<String, Object> body) {
        String id = str(body.get("id"));
        if (id == null || id.isBlank()) {
            id = str(body.get("ID"));
        }
        Doctor doctor;
        if (id != null && !id.isBlank()) {
            doctor = doctorRepository.findById(id.trim())
                    .orElseThrow(() -> new IllegalArgumentException("医生不存在"));
        } else {
            doctor = new Doctor();
            doctor.setId(UUID.randomUUID().toString().replace("-", ""));
            doctor.setCreateDate(new Date());
            doctor.setIsOnDuty(1);
            doctor.setDisplayStatus(1);
            doctor.setMustChangePassword(1);
            doctor.setPasswordResetRequested(0);
        }
        applyMap(doctor, body);
        validateWorkNoUnique(doctor);
        if (doctor.getDisplayStatus() == null) {
            doctor.setDisplayStatus(1);
        }
        if (doctor.getIsOnDuty() == null) {
            doctor.setIsOnDuty(1);
        }
        if (doctor.getCreateDate() == null) {
            doctor.setCreateDate(new Date());
        }

        List<String> deptIds = extractDeptIds(body);
        for (String deptId : deptIds) {
            if (!departmentService.canAttachResources(deptId)) {
                throw new IllegalArgumentException("科室必须是叶子节点（无子诊区）：" + deptId);
            }
        }
        Doctor saved = doctorRepository.save(doctor);
        replaceDoctorDepts(saved.getId(), deptIds);
        if (deptIds.isEmpty()) {
            saved.setDeptId(null);
            doctorRepository.save(saved);
            pendingIssueService.raise(PendingIssue.TYPE_DOCTOR_NO_DEPT,
                    "医生未绑定科室：" + (saved.getName() != null ? saved.getName() : saved.getId()),
                    "请在医生档案中选择所属科室",
                    saved.getId(), null);
        } else {
            saved.setDeptId(deptIds.get(0));
            doctorRepository.save(saved);
            pendingIssueService.resolveByTypeAndRef(PendingIssue.TYPE_DOCTOR_NO_DEPT, saved.getId());
        }
        if (saved.getUserNo() == null || saved.getUserNo().isBlank()) {
            saved.setUserNo(saved.getWorkNo());
        }
        stationUserSyncService.ensureUser(saved);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id 不能为空");
        }
        if (doctorScheduleRepository.existsByDoctorId(id.trim())) {
            throw new IllegalArgumentException("该医生存在排班记录，请先删除排班后再删除医生");
        }
        doctorDeptRepository.deleteByDoctorId(id.trim());
        pendingIssueService.resolveByTypeAndRef(PendingIssue.TYPE_DOCTOR_NO_DEPT, id.trim());
        doctorRepository.deleteById(id.trim());
    }

    /**
     * 重写医生所属科室。先删旧关联并 flush，再插入。
     * doctor_dept 主键是 IDENTITY，save 会立刻 INSERT；若 DELETE 还在持久化上下文里排队，
     * 会与仍在库中的 (doctor_id, dept_id) 冲突，触发 uk_doctor_dept。
     */
    private void replaceDoctorDepts(String doctorId, List<String> deptIds) {
        doctorDeptRepository.deleteByDoctorId(doctorId);
        doctorDeptRepository.flush();
        for (String deptId : deptIds) {
            DoctorDept dd = new DoctorDept();
            dd.setDoctorId(doctorId);
            dd.setDeptId(deptId);
            doctorDeptRepository.save(dd);
        }
    }

    private List<String> extractDeptIds(Map<String, Object> body) {
        Object raw = body.get("deptIds");
        if (raw == null) {
            raw = body.get("DeptIds");
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (raw instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null && !o.toString().isBlank()) {
                    ids.add(o.toString().trim());
                }
            }
        } else if (raw instanceof String s && !s.isBlank()) {
            for (String part : s.split("[,;]")) {
                if (!part.isBlank()) ids.add(part.trim());
            }
        }
        if (ids.isEmpty()) {
            String single = str(body.get("deptId"));
            if (single != null && !single.isBlank()) {
                ids.add(single.trim());
            }
        }
        return new ArrayList<>(ids);
    }

    private Map<String, Object> toRow(Doctor d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("workNo", d.getWorkNo());
        m.put("name", d.getName());
        m.put("title", d.getTitle());
        m.put("avatar", d.getAvatar());
        m.put("remarks", d.getRemarks());
        m.put("specialty", d.getSpecialty());
        m.put("deptId", d.getDeptId());
        m.put("deptIds", doctorDeptRepository.findByDoctorId(d.getId()).stream()
                .map(DoctorDept::getDeptId).collect(Collectors.toList()));
        m.put("gender", d.getGender());
        m.put("phone", d.getPhone());
        m.put("displayStatus", d.getDisplayStatus());
        m.put("isOnDuty", d.getIsOnDuty());
        m.put("passwordResetRequested", d.getPasswordResetRequested());
        m.put("officeName", d.getOfficeName());
        m.put("createDate", d.getCreateDate());
        return m;
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            Path dir = DoctorAvatarResourceConfig.avatarUploadDir();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "_" + Objects.requireNonNullElse(file.getOriginalFilename(), "img");
            Path target = dir.resolve(name);
            Files.copy(file.getInputStream(), target);
            return "/uploads/intros/" + name;
        } catch (IOException e) {
            return "";
        }
    }

    private void validateWorkNoUnique(Doctor doctor) {
        String workNo = doctor.getWorkNo();
        if (workNo == null || workNo.isBlank()) {
            throw new IllegalArgumentException("工号不能为空");
        }
        List<Doctor> dup = doctorRepository.findByWorkNo(workNo.trim());
        for (Doctor other : dup) {
            if (!other.getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("工号已存在：" + workNo);
            }
        }
    }

    private static void applyMap(Doctor d, Map<String, Object> body) {
        putStr(body, "workNo", "ASID", d::setWorkNo);
        putStr(body, "name", "Intro2", d::setName);
        putStr(body, "title", "Intro3", d::setTitle);
        putStr(body, "avatar", "Intro1", d::setAvatar);
        putStr(body, "remarks", "Remarks", d::setRemarks);
        putStr(body, "specialty", "Specialty", d::setSpecialty);
        putStr(body, "gender", null, d::setGender);
        putStr(body, "phone", null, d::setPhone);
        putStr(body, "screenId", null, d::setScreenId);
        putStr(body, "officeName", null, d::setOfficeName);
        if (body.get("displayStatus") != null || body.get("Status") != null) {
            Object v = body.get("displayStatus") != null ? body.get("displayStatus") : body.get("Status");
            d.setDisplayStatus(parseDisplayStatus(v));
        }
        if (body.get("isOnDuty") != null) {
            d.setIsOnDuty(parseInt(body.get("isOnDuty"), d.getIsOnDuty() != null ? d.getIsOnDuty() : 1));
        }
    }

    private static void putStr(Map<String, Object> body, String key, String legacyKey, java.util.function.Consumer<String> setter) {
        Object v = body.get(key);
        if (v == null && legacyKey != null) {
            v = body.get(legacyKey);
        }
        if (v != null) {
            setter.accept(v.toString().trim());
        }
    }

    private static int parseDisplayStatus(Object v) {
        if (v == null) {
            return 1;
        }
        String s = v.toString().trim();
        if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
            return 0;
        }
        return 1;
    }

    private static String extractStr(Map<String, Object> query, String key, String legacyKey) {
        Object v = query.get(key);
        if (v == null && legacyKey != null) {
            v = query.get(legacyKey);
        }
        return v == null ? "" : v.toString().trim();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static int parseInt(Object o, int def) {
        if (o == null) return def;
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }
}
