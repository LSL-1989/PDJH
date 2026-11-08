package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.DepartmentInput;
import com.gocent.pdjh.dto.DepartmentOutput;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.service.DepartmentService;
import com.gocent.pdjh.service.HisSyncService;
import com.gocent.pdjh.service.ScreenDeptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final ScreenDeptMappingService mappingService;
    private final HisSyncService hisSyncService;

    // 获取所有的诊室信息
    @PostMapping("/GetAllDepartment")
    public ResultData<List<DepartmentOutput>> getAllDepartment() {
        return ResultData.success(departmentService.getAllDepartment(), "获取成功");
    }
    
    // (保留旧版名称) 根据传回来的屏幕ID，返回未关联的和所有的科室供前台分配穿梭框
    @PostMapping("/GetDepartments")
    public ResultData<List<DepartmentOutput>> getDepartments(@RequestBody ScreensInput input) {
        return ResultData.success(mappingService.getDepartmentsWithScreenState(input), "获取成功");
    }

    // 修改诊室信息
    @PostMapping("/EditorDepartment")
    public ResultData<Boolean> editorDepartment(@RequestBody DepartmentInput input) {
        try {
            boolean ok = departmentService.editorDepartment(input);
            return ok ? ResultData.success(true, "修改成功") : ResultData.fail(500, "修改失败");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    // 新增诊室
    @PostMapping("/CreateDepartment")
    public ResultData<Boolean> createDepartment(@RequestBody DepartmentInput input) {
        try {
            boolean ok = departmentService.createDepartment(input);
            return ok ? ResultData.success(true, "新增成功") : ResultData.fail(500, "新增失败");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(409, e.getMessage());
        }
    }

    // 删除诊室（兼容 query 参数 ID 与 JSON body { "ID": "..." }）
    @PostMapping("/RemoveDepartments")
    public ResultData<Boolean> removeDepartments(
            @RequestParam(value = "ID", required = false) String idParam,
            @RequestBody(required = false) DepartmentInput input) {
        String id = idParam;
        if ((id == null || id.isBlank()) && input != null) {
            id = input.getId();
        }
        if (id == null || id.isBlank()) {
            return ResultData.fail(500, "参数缺失：科室ID不能为空");
        }
        try {
            boolean ok = departmentService.removeDepartment(id);
            return ok ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败，请稍后重试");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    @PostMapping("/GetUserParentDeptID")
    public ResultData<List<DepartmentOutput>> getUserParentDeptID(@RequestBody DepartmentInput input) {
        if (input == null || input.getId() == null) {
            return ResultData.fail(500, "参数缺失");
        }
        return ResultData.success(departmentService.listDepartmentsForUserParent(input.getId()), "获取成功");
    }

    @PostMapping("/syncFromHis")
    public ResultData<Map<String, Object>> syncFromHis(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = hisSyncService.saveAndSync(HisSyncService.CONFIG_KEY_DEPT, body, true);
            return ResultData.success(result, String.valueOf(result.get("message")));
        } catch (Exception e) {
            return ResultData.fail(500, e.getMessage());
        }
    }

    @PostMapping("/probeHisSync")
    public ResultData<Map<String, Object>> probeHisSync(@RequestBody Map<String, Object> body) {
        return ResultData.success(hisSyncService.probe(HisSyncService.CONFIG_KEY_DEPT, body, true), "拨测完成");
    }

    @GetMapping("/getHisSyncConfig")
    public ResultData<Map<String, Object>> getHisSyncConfig() {
        return ResultData.success(hisSyncService.getSyncConfig(HisSyncService.CONFIG_KEY_DEPT), "获取成功");
    }
}
