package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.DictDoctorTitle;
import com.gocent.pdjh.service.DoctorTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 医生职称字典接口。
 * 消费者：管理后台医生职称页、医生档案职称下拉。与 Doctor/Save 不同：本接口维护可选名称，不写医生档案。
 */
@RestController
@RequestMapping("/DoctorTitle")
@RequiredArgsConstructor
public class DoctorTitleController {

    private final DoctorTitleService doctorTitleService;

    /** 全部职称，按排序返回；下拉端自行过滤 enabled=1 */
    @PostMapping("/GetAll")
    public ResultData<List<DictDoctorTitle>> getAll() {
        return ResultData.success(doctorTitleService.listAll(), "获取成功");
    }

    /** 新增或修改。body：id（空则新增）、name、sortOrder、enabled */
    @PostMapping("/Save")
    public ResultData<DictDoctorTitle> save(@RequestBody DictDoctorTitle body) {
        try {
            return ResultData.success(doctorTitleService.save(body), "保存成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /** 删除。body：{ id }；仍被医生使用时返回失败说明 */
    @PostMapping("/Delete")
    public ResultData<Boolean> delete(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null) {
            return ResultData.fail(400, "id 不能为空");
        }
        try {
            doctorTitleService.delete(Integer.valueOf(idObj.toString()));
            return ResultData.success(true, "删除成功");
        } catch (NumberFormatException e) {
            return ResultData.fail(400, "id 无效");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }
}
