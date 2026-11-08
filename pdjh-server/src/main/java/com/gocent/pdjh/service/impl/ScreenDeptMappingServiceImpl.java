package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.DepartmentOutput;
import com.gocent.pdjh.dto.ScreenDeptMappingInput;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.ScreenDeptMapping;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.ScreenDeptMappingRepository;
import com.gocent.pdjh.service.ScreenDeptMappingService;
import com.gocent.pdjh.service.ScreenLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenDeptMappingServiceImpl implements ScreenDeptMappingService {

    private final ScreenDeptMappingRepository mappingRepository;
    private final DepartmentRepository departmentRepository;
    private final ScreenLifecycleService screenLifecycleService;

    @Override
    @Transactional
    public boolean relevancyScreenAndDepts(ScreenDeptMappingInput input) {
        if (input == null || input.getScreenInput() == null) return false;

        try {
            String screenId = input.getScreenInput();
            
            // 先清除该屏幕原有的所有关联
            mappingRepository.deleteByScreenId(screenId);

            // 重新插入新关联
            if (input.getDepartmentInputKeys() != null) {
                List<ScreenDeptMapping> mappings = input.getDepartmentInputKeys().stream().map(deptId -> {
                    ScreenDeptMapping mapping = new ScreenDeptMapping();
                    mapping.setScreenId(screenId);
                    mapping.setDeptId(deptId);
                    return mapping;
                }).collect(Collectors.toList());

                mappingRepository.saveAll(mappings);
            }
            log.info("屏幕与诊室关联更新成功，屏幕ID: {}", screenId);
            screenLifecycleService.refreshAfterBinding(screenId);
            return true;
        } catch (Exception e) {
            log.error("屏幕与诊室关联失败", e);
            return false;
        }
    }

    @Override
    public List<DepartmentOutput> getHasassociatedDepartments(ScreensInput screensInput) {
        if (screensInput == null || screensInput.getId() == null) return List.of();
        
        // 查找屏幕关联的所有映射
        List<ScreenDeptMapping> mappings = mappingRepository.findByScreenId(screensInput.getId());
        List<String> deptIds = mappings.stream().map(ScreenDeptMapping::getDeptId).collect(Collectors.toList());

        // 查找对应的科室实体并转换为 DTO
        if (deptIds.isEmpty()) return List.of();
        List<Department> depts = departmentRepository.findAllById(deptIds);

        return depts.stream().map(dept -> {
            DepartmentOutput out = new DepartmentOutput();
            BeanUtils.copyProperties(dept, out);
            out.setDisabled(false);
            return out;
        }).collect(Collectors.toList());
    }

    @Override
    public List<DepartmentOutput> getDepartmentsWithScreenState(ScreensInput screensInput) {
        String queryScreenId = screensInput != null ? screensInput.getId() : null;

        // 获取全部诊室
        List<Department> allDepts = departmentRepository.findAll();
        
        // 获取指定屏幕已关联的科室ID集合
        List<String> associatedDeptIds = List.of();
        if (queryScreenId != null) {
            associatedDeptIds = mappingRepository.findByScreenId(queryScreenId)
                    .stream().map(ScreenDeptMapping::getDeptId).collect(Collectors.toList());
        }

        final List<String> fIds = associatedDeptIds;
        return allDepts.stream().map(dept -> {
            DepartmentOutput out = new DepartmentOutput();
            BeanUtils.copyProperties(dept, out);
            // 这里原来代码逻辑其实略复杂，我们统一为： disabled = true 表示该屏幕 "不可再被选 (比如已关联其他)"，
            // 或是已经选择状态由于不同前端组件处理不同，我们先根据已关联来打标为 true，没关联为 false
            out.setDisabled(fIds.contains(dept.getId())); 
            return out;
        }).collect(Collectors.toList());
    }
}
