package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.CallAutoNumSyncResult;
import com.gocent.pdjh.entity.CallAutoNum;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.SysMenu;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.repository.CallAutoNumRepository;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.SysMenuRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.CallAutoNumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CallAutoNumServiceImpl implements CallAutoNumService {

    /** 主任工作台子菜单 pageUrl → 科室 businessType */
    private static final Map<String, String> DIRECTOR_MENU_TO_BUSINESS = Map.of(
            "outpatientDesk", "OUTPATIENT",
            "pharmacyDesk", "PHARMACY",
            "examDesk", "EXAM",
            "labDesk", "LAB",
            "medtechDesk", "MEDTECH"
    );

    private final CallAutoNumRepository repository;
    private final DepartmentRepository departmentRepository;
    private final CallListRepository callListRepository;
    private final SysMenuRepository sysMenuRepository;
    private final UserRepository userRepository;

    @Override
    public List<CallAutoNum> listAll() {
        return repository.findAll();
    }

    @Override
    public boolean create(Map<String, Object> body) {
        CallAutoNum e = new CallAutoNum();
        e.setGid(UUID.randomUUID().toString());
        fill(e, body);
        repository.save(e);
        return true;
    }

    @Override
    public boolean update(Map<String, Object> body) {
        Object gid = body.get("GID");
        if (gid == null) {
            return false;
        }
        Optional<CallAutoNum> opt = repository.findById(gid.toString());
        if (opt.isEmpty()) {
            return false;
        }
        CallAutoNum e = opt.get();
        fill(e, body);
        repository.save(e);
        return true;
    }

    @Override
    public boolean remove(String gid) {
        if (gid == null || gid.isEmpty()) {
            return false;
        }
        repository.deleteById(gid);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallAutoNumSyncResult syncFromCallList(String token, String autoDate) {
        Users user = resolveUser(token);
        LocalDate syncDate = parseSyncDate(autoDate);

        Set<String> businessTypes = resolveBusinessTypesByRole(user.getRoleId());
        CallAutoNumSyncResult result = new CallAutoNumSyncResult();
        result.setSyncDate(syncDate.toString());
        result.setBusinessTypes(new ArrayList<>(businessTypes));

        if (businessTypes.isEmpty()) {
            return result;
        }

        List<Department> departments = departmentRepository.findByBusinessTypeIn(businessTypes).stream()
                .filter(this::isActiveDepartment)
                .collect(Collectors.toList());
        result.setDeptCount(departments.size());
        if (departments.isEmpty()) {
            return result;
        }

        List<String> deptIds = departments.stream().map(Department::getId).collect(Collectors.toList());
        Map<String, int[]> maxByDept = loadMaxXhByDept(deptIds, syncDate);

        int created = 0;
        int updated = 0;
        for (Department dept : departments) {
            int[] maxes = maxByDept.getOrDefault(dept.getId(), new int[] {0, 0, 0});
            Optional<CallAutoNum> existingOpt = repository.findFirstByDidAndAutoDate(dept.getId(), syncDate);
            if (existingOpt.isPresent()) {
                CallAutoNum existing = existingOpt.get();
                existing.setTypeA(maxes[0]);
                existing.setTypeB(maxes[1]);
                existing.setTypeC(maxes[2]);
                existing.setName(dept.getName());
                existing.setCode(dept.getBusinessType() != null ? dept.getBusinessType() : "QUEUE");
                repository.save(existing);
                updated++;
            } else {
                CallAutoNum createdRow = new CallAutoNum();
                createdRow.setGid(UUID.randomUUID().toString());
                createdRow.setDid(dept.getId());
                createdRow.setName(dept.getName());
                createdRow.setCode(dept.getBusinessType() != null ? dept.getBusinessType() : "QUEUE");
                createdRow.setAutoDate(syncDate);
                createdRow.setTypeA(maxes[0]);
                createdRow.setTypeB(maxes[1]);
                createdRow.setTypeC(maxes[2]);
                repository.save(createdRow);
                created++;
            }
        }

        result.setCreatedCount(created);
        result.setUpdatedCount(updated);
        result.setSyncedCount(created + updated);
        return result;
    }

    private Users resolveUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token 无效");
        }
        return userRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Token 无效或已过期"));
    }

    private static LocalDate parseSyncDate(String autoDate) {
        if (autoDate == null || autoDate.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(autoDate.trim().substring(0, 10));
    }

    private Set<String> resolveBusinessTypesByRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return Set.of();
        }
        List<SysMenu> menus = sysMenuRepository.findMenusByRoleId(roleId);
        Set<String> types = new LinkedHashSet<>();
        for (SysMenu menu : menus) {
            String pageUrl = menu.getPageUrl();
            if (pageUrl == null) {
                continue;
            }
            String businessType = DIRECTOR_MENU_TO_BUSINESS.get(pageUrl.trim());
            if (businessType != null) {
                types.add(businessType);
            }
        }
        return types;
    }

    private boolean isActiveDepartment(Department dept) {
        return dept.getState() == null || dept.getState() != 0;
    }

    private Map<String, int[]> loadMaxXhByDept(List<String> deptIds, LocalDate syncDate) {
        Map<String, int[]> map = new HashMap<>();
        if (deptIds.isEmpty()) {
            return map;
        }
        List<Object[]> rows = callListRepository.findMaxXhGroupByDeptAndXhlbForDate(deptIds, syncDate);
        for (Object[] row : rows) {
            if (row == null || row.length < 4 || row[0] == null) {
                continue;
            }
            String deptId = row[0].toString();
            map.put(deptId, new int[] {
                    toInt(row[1]),
                    toInt(row[2]),
                    toInt(row[3])
            });
        }
        return map;
    }

    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private void fill(CallAutoNum e, Map<String, Object> body) {
        String deptId = str(body.get("DeptID"));
        e.setDid(deptId);
        departmentRepository.findById(deptId).ifPresent(d -> {
            e.setName(d.getName());
            e.setCode(d.getBusinessType() != null ? d.getBusinessType() : "QUEUE");
        });
        if (body.get("AutoDate") != null) {
            e.setAutoDate(LocalDate.parse(body.get("AutoDate").toString().substring(0, 10)));
        }
        if (body.get("TypeA") != null) {
            e.setTypeA(Integer.parseInt(body.get("TypeA").toString()));
        }
        if (body.get("TypeB") != null) {
            e.setTypeB(Integer.parseInt(body.get("TypeB").toString()));
        }
        if (body.get("TypeC") != null) {
            e.setTypeC(Integer.parseInt(body.get("TypeC").toString()));
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
