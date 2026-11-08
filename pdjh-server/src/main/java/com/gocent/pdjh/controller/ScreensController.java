package com.gocent.pdjh.controller;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.dto.ScreensOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;
import com.gocent.pdjh.service.ScreensService;
import com.gocent.pdjh.service.TerminalService;
import com.gocent.pdjh.service.WaitTimeEstimatorService;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.vo.GlobalSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Screens")
@RequiredArgsConstructor
public class ScreensController {

    private final ScreensService screensService;
    private final TerminalService terminalService;
    private final DocScreenRepository docScreenRepository;
    private final com.gocent.pdjh.repository.CallListRepository callListRepository;
    private final DepartmentRepository departmentRepository;
    private final WaitTimeEstimatorService waitTimeEstimatorService;

    @PostMapping("/GetScreens")
    public ResultData<List<ScreensOutput>> getScreens(@RequestBody ScreensInput input) {
        List<ScreensOutput> list = screensService.getScreens(input);
        return ResultData.success(list, "获取所有大屏成功");
    }

    @PostMapping("/GetScreensSingle")
    public ResultData<ScreensOutput> getScreensSingle(@RequestBody ScreensInput input) {
        if (input.getId() == null) {
            return ResultData.fail(500, "ID 不能为空");
        }
        ScreensOutput single = screensService.getScreensSingle(input.getId());
        return ResultData.success(single, "获取单个大屏详情成功");
    }

    @PostMapping("/GetScreensInput")
    public ResultData<ScreensOutput> getScreensInput(@RequestBody java.util.Map<String, Object> body) {
        String screenId = null;
        if (body.containsKey("ScreensInput")) {
            Object inputObj = body.get("ScreensInput");
            if (inputObj instanceof java.util.Map) {
                screenId = (String)((java.util.Map<?, ?>)inputObj).get("ID");
            }
        }
        if (screenId == null && body.containsKey("ID")) {
            screenId = (String)body.get("ID");
        }
        if (screenId == null) {
            return ResultData.fail(500, "无法获取大屏ID");
        }
        ScreensOutput single = screensService.getScreensSingle(screenId);
        return ResultData.success(single, "获取大屏配置成功");
    }

    @PostMapping("/UpdateScreen")
    public ResultData<Boolean> updateScreen(@RequestBody ScreensInput input) {
        boolean ok = screensService.updateScreen(input);
        return ok ? ResultData.success(true, "更新成功") : ResultData.fail(500, "更新失败");
    }

    @PostMapping("/AddScreen")
    public ResultData<Boolean> addScreen(@RequestBody ScreensInput input) {
        boolean ok = screensService.addScreen(input);
        return ok ? ResultData.success(true, "添加成功") : ResultData.fail(500, "添加失败");
    }

    @PostMapping("/DelScreen")
    public ResultData<Boolean> delScreen(@RequestBody ScreensInput input) {
        if (input.getId() == null) {
            return ResultData.fail(500, "ID 不能为空");
        }
        boolean ok = screensService.delScreen(input.getId());
        return ok ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败");
    }

    /**
     * 【候诊区大屏 SCREEN 壳专用，已废弃】自动分发路由。
     * CLINIC 门边屏请使用 Terminal/Resolve + /clinic/ 入口，勿调用本接口。
     */
    @Deprecated
    @GetMapping("/GetDeviceRoute")
    public ResultData<ScreensOutput> getDeviceRoute(@RequestParam String deviceCode) {
        if (deviceCode == null || deviceCode.isEmpty()) {
            return ResultData.fail(500, "设备识别码(deviceCode)不能为空");
        }
        DocScreen screen = docScreenRepository.findByDeviceCode(deviceCode.trim()).orElse(null);
        if (screen != null && TerminalConstants.TYPE_CLINIC.equals(screen.getTerminalType())) {
            return ResultData.fail(400, "该设备为诊室门边屏(CLINIC)，请使用 CLINIC 壳访问 /clinic/ 并通过 Terminal/Resolve 解析");
        }
        ResultData<TerminalResolveOutput> resolved = terminalService.resolve(deviceCode, TerminalConstants.TYPE_SCREEN);
        if (!resolved.isSuccess()) {
            if (resolved.getCode() == 428 && resolved.getData() != null) {
                return new ResultData<>(false, 428, resolved.getMsg(), null);
            }
            return ResultData.fail(resolved.getCode(), resolved.getMsg());
        }
        ScreensOutput output = screensService.getScreenByDeviceCode(deviceCode);
        if (output == null) {
            return ResultData.fail(404, "设备未注册，请在管理后台中绑定屏幕标识码：" + deviceCode);
        }
        TerminalResolveOutput meta = resolved.getData();
        if (meta != null) {
            output.setTemplateRoute(meta.getTemplateRoute());
            if (meta.getContentConfig() != null) {
                output.setContentConfig(meta.getContentConfig());
            }
        }
        return ResultData.success(output, "获取全量路由信息成功");
    }

    /**
     * 【全院大屏专用】获取全院所有科室的等候人数汇总
     */
    @GetMapping("/GlobalSummary")
    public ResultData<List<GlobalSummaryVO>> getGlobalSummary() {
        List<com.gocent.pdjh.dto.DeptWaitSummary> summary = callListRepository.findGlobalWaitingSummary();
        
        List<GlobalSummaryVO> result = summary.stream().map(s -> {
            GlobalSummaryVO vo = new GlobalSummaryVO();
            vo.setDeptId(s.getDeptId());
            vo.setWaitCount(s.getWaitCount() != null ? s.getWaitCount() : 0);
            
            Department dept = departmentRepository.findById(s.getDeptId()).orElse(null);
            if (dept != null) {
                vo.setDeptName(dept.getName());
            } else {
                vo.setDeptName("科室代码: " + s.getDeptId());
            }
            
            // P4: Inject dynamic P2 algorithm instead of hardcoded 3 minutes
            int estimated = waitTimeEstimatorService.estimateWaitMinutes(dept, vo.getWaitCount());
            vo.setEstimatedWaitMinutes(estimated);
            
            // Re-use heatmap status thresholds for extra functionality
            if (vo.getWaitCount() >= 15) {
                vo.setStatus("CONGESTED");
            } else if (vo.getWaitCount() >= 5) {
                vo.setStatus("BUSY");
            } else {
                vo.setStatus("IDLE");
            }
            return vo;
        }).collect(Collectors.toList());
        
        return ResultData.success(result, "获取全院等待人数统计成功");
    }
}
