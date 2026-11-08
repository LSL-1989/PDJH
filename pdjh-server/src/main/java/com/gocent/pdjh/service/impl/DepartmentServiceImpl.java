package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.DepartmentInput;
import com.gocent.pdjh.dto.DepartmentOutput;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.PendingIssue;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.DoctorDeptRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import com.gocent.pdjh.repository.MedTechSlotRepository;
import com.gocent.pdjh.repository.NurseScheduleRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.ScreenDeptMappingRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.DepartmentService;
import com.gocent.pdjh.service.PendingIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 科室/诊区维护：支持多层；仅叶子可挂医生/诊室；手工 ID 与 HIS 并存；删除约束。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    public static final String ROOT_FATHER = "00000000-0000-0000-0000-000000000000";
    /** Type=1 分支（诊区/分组）；Type=2 叶子科室（可挂医生/诊室/队列） */
    public static final int TYPE_BRANCH = 1;
    public static final int TYPE_LEAF = 2;

    private final DepartmentRepository departmentRepository;
    private final ScreenDeptMappingRepository screenDeptMappingRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorDeptRepository doctorDeptRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final UserRepository userRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final NurseScheduleRepository nurseScheduleRepository;
    private final MedTechSlotRepository medTechSlotRepository;
    private final PendingIssueService pendingIssueService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<DepartmentOutput> getAllDepartment() {
        List<Department> list = departmentRepository.findAll();
        Set<String> parentIdsWithChildren = list.stream()
                .map(Department::getFatherID)
                .filter(f -> f != null && !f.isBlank() && !ROOT_FATHER.equalsIgnoreCase(f))
                .collect(Collectors.toSet());
        return list.stream().map(dept -> toOutput(dept, !parentIdsWithChildren.contains(dept.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDepartment(DepartmentInput input) {
        if (input == null || input.getName() == null || input.getName().isBlank()) {
            throw new IllegalArgumentException("科室/诊区名称不能为空");
        }
        String name = input.getName().trim();
        Integer type = input.getType() != null ? input.getType() : TYPE_LEAF;
        String fatherId = normalizeFather(input.getFatherID());

        String id = input.getId() != null ? input.getId().trim() : "";
        if (id.isEmpty()) {
            if (type == TYPE_LEAF) {
                throw new IllegalArgumentException("叶子科室必须手工录入与 HIS 一致的科室 ID");
            }
            id = UUID.randomUUID().toString();
        }

        if (departmentRepository.existsById(id)) {
            pendingIssueService.raise(PendingIssue.TYPE_DEPT_ID_CONFLICT,
                    "科室 ID 冲突：" + id,
                    "手工录入或同步时 ID 已存在，名称尝试写入：" + name,
                    id, null);
            throw new IllegalArgumentException("科室 ID 已存在：" + id + "，已记入首页待处理");
        }
        List<Department> sameName = departmentRepository.findByName(name);
        if (!sameName.isEmpty()) {
            pendingIssueService.raise(PendingIssue.TYPE_DEPT_NAME_CONFLICT,
                    "科室名称冲突：" + name,
                    "已存在同名节点，新 ID 尝试写入：" + id,
                    sameName.get(0).getId(), null);
            throw new IllegalArgumentException("科室名称已存在：" + name + "，已记入首页待处理");
        }

        if (!ROOT_FATHER.equalsIgnoreCase(fatherId) && !departmentRepository.existsById(fatherId)) {
            throw new IllegalArgumentException("父级诊区/科室不存在：" + fatherId);
        }

        Department dept = new Department();
        dept.setId(id);
        dept.setName(name);
        dept.setType(type);
        dept.setFatherID(fatherId);
        dept.setState(input.getState() != null ? input.getState() : 1);
        dept.setQueue(type == TYPE_LEAF ? strOrEmpty(input.getQueue()) : null);
        dept.setBusinessType(input.getBusinessType());
        dept.setIsSuspended(input.getIsSuspended() != null ? input.getIsSuspended() : 0);
        dept.setMedTechSubType(input.getMedTechSubType());
        departmentRepository.save(dept);
        log.info("创建科室/诊区成功: id={}, name={}, type={}", id, name, type);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editorDepartment(DepartmentInput input) {
        if (input.getId() == null) return false;

        Department existing = departmentRepository.findById(input.getId()).orElse(null);
        if (existing == null) return false;

        Integer prevState = existing.getState();
        Integer prevSuspended = existing.getIsSuspended();

        if (input.getName() != null) {
            String newName = input.getName().trim();
            List<Department> sameName = departmentRepository.findByName(newName);
            boolean conflict = sameName.stream().anyMatch(d -> !d.getId().equals(existing.getId()));
            if (conflict) {
                pendingIssueService.raise(PendingIssue.TYPE_DEPT_NAME_CONFLICT,
                        "科室名称冲突：" + newName,
                        "编辑时与已有节点重名，当前 ID：" + existing.getId(),
                        existing.getId(), null);
                throw new IllegalArgumentException("科室名称已存在：" + newName);
            }
            existing.setName(newName);
        }
        if (input.getState() != null) existing.setState(input.getState());
        if (input.getQueue() != null) existing.setQueue(input.getQueue());
        if (input.getBusinessType() != null) existing.setBusinessType(input.getBusinessType());
        if (input.getIsSuspended() != null) existing.setIsSuspended(input.getIsSuspended());
        if (input.getMedTechSubType() != null) existing.setMedTechSubType(input.getMedTechSubType());

        departmentRepository.save(existing);
        notifyNurseIfClosedOrSuspended(existing, prevState, prevSuspended);
        log.info("更新科室成功: {}", existing.getName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDepartment(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Optional<Department> deptOpt = departmentRepository.findById(id);
        if (deptOpt.isEmpty()) {
            throw new IllegalArgumentException("科室不存在");
        }
        List<Department> children = departmentRepository.findByFatherID(id);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该节点下仍有 " + children.size() + " 个子科室/诊区，不可删除");
        }

        List<Doctor> boundDoctors = new ArrayList<>(doctorRepository.findByDeptIdOrderByTitleAsc(id));
        doctorDeptRepository.findByDeptId(id).forEach(dd ->
                doctorRepository.findById(dd.getDoctorId()).ifPresent(boundDoctors::add));

        Set<String> doctorIds = new HashSet<>();
        for (Doctor d : boundDoctors) {
            if (d.getId() != null) doctorIds.add(d.getId());
        }
        if (!doctorIds.isEmpty()) {
            log.info("删除科室 {} 将解绑 {} 名医生", id, doctorIds.size());
            for (String doctorId : doctorIds) {
                doctorDeptRepository.deleteByDoctorIdAndDeptId(doctorId, id);
                doctorRepository.findById(doctorId).ifPresent(doc -> {
                    if (id.equals(doc.getDeptId())) {
                        doc.setDeptId(null);
                        doctorRepository.save(doc);
                        pendingIssueService.raise(PendingIssue.TYPE_DOCTOR_NO_DEPT,
                                "医生未绑定科室：" + (doc.getName() != null ? doc.getName() : doc.getId()),
                                "因删除科室 " + id + " 解绑",
                                doc.getId(), null);
                    }
                });
            }
        }

        detachDeptReferences(id);
        departmentRepository.deleteById(id);
        pendingIssueService.resolveByTypeAndRef(PendingIssue.TYPE_DEPT_ID_CONFLICT, id);
        pendingIssueService.resolveByTypeAndRef(PendingIssue.TYPE_DEPT_NAME_CONFLICT, id);
        log.info("删除科室成功: {}", id);
        return true;
    }

    private void detachDeptReferences(String deptId) {
        screenDeptMappingRepository.deleteByDeptId(deptId);
        officeRoomRepository.findByDeptId(deptId).forEach(officeRoomRepository::delete);
        doctorScheduleRepository.deleteByDeptId(deptId);
        nurseScheduleRepository.deleteByDeptId(deptId);
        medTechSlotRepository.findByDeptId(deptId).forEach(medTechSlotRepository::delete);
        for (Users user : userRepository.findByDeptID(deptId)) {
            user.setDeptID(null);
            userRepository.save(user);
        }
    }

    @Override
    public List<DepartmentOutput> listDepartmentsForUserParent(String seedDeptId) {
        if (seedDeptId == null || seedDeptId.isEmpty()) {
            return List.of();
        }
        Optional<Department> dOpt = departmentRepository.findById(seedDeptId);
        if (dOpt.isEmpty()) {
            return List.of();
        }
        Department d = dOpt.get();
        String zoneId = hasParent(d.getFatherID()) ? d.getFatherID() : d.getId();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add(zoneId);
        collectDescendants(zoneId, ids);
        Set<String> parentsWithChildren = departmentRepository.findAll().stream()
                .map(Department::getFatherID)
                .filter(this::hasParent)
                .collect(Collectors.toSet());
        return ids.stream()
                .map(departmentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(dept -> toOutput(dept, !parentsWithChildren.contains(dept.getId())))
                .collect(Collectors.toList());
    }

    /** 递归收集子节点（含多层） */
    @Override
    public void collectDescendants(String parentId, Set<String> ids) {
        for (Department ch : departmentRepository.findByFatherID(parentId)) {
            if (ids.add(ch.getId())) {
                collectDescendants(ch.getId(), ids);
            }
        }
    }

    public boolean isLeafDepartment(String deptId) {
        return canAttachResources(deptId);
    }

    /** 是否可作为医生/诊室挂载目标：节点存在且无子节点 */
    @Override
    public boolean canAttachResources(String deptId) {
        if (deptId == null || deptId.isBlank()) return false;
        if (!departmentRepository.existsById(deptId)) return false;
        return departmentRepository.findByFatherID(deptId).isEmpty();
    }

    private void notifyNurseIfClosedOrSuspended(Department dept, Integer prevState, Integer prevSuspended) {
        boolean closedNow = dept.getState() != null && dept.getState() == 0;
        boolean wasClosed = prevState != null && prevState == 0;
        boolean suspendedNow = Integer.valueOf(1).equals(dept.getIsSuspended());
        boolean wasSuspended = Integer.valueOf(1).equals(prevSuspended);

        if ((closedNow && !wasClosed) || (suspendedNow && !wasSuspended) || (!closedNow && wasClosed) || (!suspendedNow && wasSuspended)) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "DEPT_STATUS");
            msg.put("deptId", dept.getId());
            msg.put("deptName", dept.getName());
            if (closedNow && !wasClosed) {
                msg.put("action", "CLOSED");
                msg.put("message", (dept.getName() != null ? dept.getName() : dept.getId()) + " 已关闭");
            } else if (!closedNow && wasClosed) {
                msg.put("action", "OPENED");
                msg.put("message", (dept.getName() != null ? dept.getName() : dept.getId()) + " 已重新启用");
            } else if (suspendedNow && !wasSuspended) {
                msg.put("action", "SUSPENDED");
                msg.put("message", (dept.getName() != null ? dept.getName() : dept.getId()) + " 已停诊");
            } else {
                msg.put("action", "RESUMED");
                msg.put("message", (dept.getName() != null ? dept.getName() : dept.getId()) + " 已恢复出诊");
            }
            messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
        }
    }

    private DepartmentOutput toOutput(Department dept, boolean isLeaf) {
        DepartmentOutput out = new DepartmentOutput();
        BeanUtils.copyProperties(dept, out);
        out.setDisabled(dept.getState() != null && dept.getState() == 0);
        out.setIsLeaf(isLeaf);
        out.setHasChildren(!isLeaf);
        return out;
    }

    private static String normalizeFather(String fatherId) {
        if (fatherId == null || fatherId.isBlank() || "true".equalsIgnoreCase(fatherId)) {
            return ROOT_FATHER;
        }
        return fatherId.trim();
    }

    private boolean hasParent(String fatherId) {
        return fatherId != null && !fatherId.isBlank() && !ROOT_FATHER.equalsIgnoreCase(fatherId);
    }

    private static String strOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
