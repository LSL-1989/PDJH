package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.DictShiftType;
import com.gocent.pdjh.service.ShiftTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 医生出诊班次字典接口。
 * 消费者：系统管理排班类型页、出诊排班班次下拉。与 DoctorSchedule/save 不同：本接口只维护可选班次，不写排班。
 */
@RestController
@RequestMapping("/ShiftType")
@RequiredArgsConstructor
public class ShiftTypeController {

    private final ShiftTypeService shiftTypeService;

    /** 全部班次，含停用；按排序返回。下拉端自行过滤 enabled=1 */
    @PostMapping("/GetAll")
    public ResultData<List<DictShiftType>> getAll() {
        return ResultData.success(shiftTypeService.listAll(), "获取成功");
    }

    /**
     * 新增或修改。
     * body：id（空则新增）、code、name、startTime、endTime、tagStyle、sortOrder、enabled、exclusiveFlag。
     */
    @PostMapping("/Save")
    public ResultData<DictShiftType> save(@RequestBody DictShiftType body) {
        try {
            return ResultData.success(shiftTypeService.save(body), "保存成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /** 删除。body：{ id }；仍被排班使用时返回失败说明 */
    @PostMapping("/Delete")
    public ResultData<Boolean> delete(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null) {
            return ResultData.fail(400, "id 不能为空");
        }
        try {
            shiftTypeService.delete(Integer.valueOf(idObj.toString()));
            return ResultData.success(true, "删除成功");
        } catch (NumberFormatException e) {
            return ResultData.fail(400, "id 无效");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }
}
