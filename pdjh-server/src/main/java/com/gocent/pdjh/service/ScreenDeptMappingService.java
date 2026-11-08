package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.DepartmentOutput;
import com.gocent.pdjh.dto.ScreenDeptMappingInput;
import com.gocent.pdjh.dto.ScreensInput;

import java.util.List;

public interface ScreenDeptMappingService {

    // 设置屏幕与诊室关联 (全量覆盖式关联)
    boolean relevancyScreenAndDepts(ScreenDeptMappingInput input);

    // 获取某大屏已关联的科室信息
    List<DepartmentOutput> getHasassociatedDepartments(ScreensInput screensInput);

    // 获取所有科室信息及 disabled 状态（该屏幕是否已关联）
    List<DepartmentOutput> getDepartmentsWithScreenState(ScreensInput screensInput);
}
