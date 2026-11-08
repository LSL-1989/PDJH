package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.DepartmentOutput;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.ScreenDeptMappingInput;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.service.ScreenDeptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ScreenDeptMapping")
@RequiredArgsConstructor
public class ScreenDeptMappingController {

    private final ScreenDeptMappingService mappingService;

    @PostMapping("/GetHasassociatedDepartments")
    public ResultData<List<DepartmentOutput>> getHasassociatedDepartments(@RequestBody ScreensInput input) {
        return ResultData.success(mappingService.getHasassociatedDepartments(input), "获取成功");
    }

    @PostMapping("/RelevancyScreenAndDepts")
    public ResultData<Boolean> relevancyScreenAndDepts(@RequestBody ScreenDeptMappingInput input) {
        boolean ok = mappingService.relevancyScreenAndDepts(input);
        return ok ? ResultData.success(true, "操作成功") : ResultData.fail(500, "操作失败");
    }
}
