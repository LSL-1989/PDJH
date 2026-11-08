package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.DocCallMsgInput;
import com.gocent.pdjh.dto.DocCallMsgInputUp;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.service.DoctorCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/DoctorCall")
@RequiredArgsConstructor
public class DoctorCallController {

    private final DoctorCallService doctorCallService;

    /**
     * 获取队列（兼容 C#：api/DoctorCall/GetDocCallList）。
     * 可选 officeRoomId：有则按诊室过滤，与门边屏口径一致。
     */
    @GetMapping("/GetDocCallList")
    public ResultData<List<CallList>> getDocCallList(
            @RequestParam("deptid") String deptId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", defaultValue = "0") Integer state,
            @RequestParam(value = "day", defaultValue = "0") Integer day,
            @RequestParam(value = "officeRoomId", required = false) String officeRoomId) {
        
        List<CallList> result = doctorCallService.getCallList(deptId, state, officeRoomId);
        return ResultData.success(result, "获取成功");
    }

    /**
     * 兼容旧版及新分离版的大厅全量分页条件检索（附带管线 businessType 硬隔离控制）
     */
    @PostMapping("/GetCallList")
    public ResultData<java.util.Map<String, Object>> getCallList(@RequestBody com.gocent.pdjh.dto.QueryInput input) {
        java.util.Map<String, Object> result = doctorCallService.getPaginatedCallList(input);
        return ResultData.success(result, "查询成功");
    }

    /**
     * 修改呼叫状态 (例如，复诊，空号，就诊结束等)
     */
    @PostMapping("/EditCallMsgState")
    public ResultData<Boolean> editCallMsgState(@RequestBody DocCallMsgInputUp input) {
        if (input.getCalllistid() == null || input.getState() == null) {
            return ResultData.fail(500, "参数缺失");
        }
        boolean success = doctorCallService.editCallMsgState(
                input.getCalllistid(),
                input.getState(),
                input.getOfficeRoomId(),
                input.getDoctorId());
        return success ? ResultData.success(true, "状态修改成功") : ResultData.fail(500, "操作失败");
    }

    /**
     * 呼叫信息处理（大屏语音播报等）
     */
    @PostMapping("/DocCallMsg")
    public ResultData<Boolean> docCallMsg(@RequestBody DocCallMsgInput input) {
        if (input.getMsg() == null) {
            return ResultData.fail(500, "参数 Msg 不能为空");
        }
        boolean success = doctorCallService.docCallMsg(input);
        return success ? ResultData.success(true, "呼叫成功") : ResultData.fail(500, "呼叫异常");
    }

    /**
     * 重载的辅助呼叫窗口信息
     */
    @PostMapping("/DocCallMsg2")
    public ResultData<Boolean> docCallMsg2(@RequestBody DocCallMsgInput input) {
        // C# 原有的逻辑：与 DocCallMsg 基本一致，除了处理数组的差别，这里简化走统一核心。
        if (input.getMsg() == null) {
            return ResultData.fail(500, "参数 Msg 不能为空");
        }
        boolean success = doctorCallService.docCallMsg(input);
        return success ? ResultData.success(true, "呼叫成功") : ResultData.fail(500, "呼叫异常");
    }

    /**
     * 紧急插队与延后 (US2 Phase 4)
     */
    @PostMapping("/EmergencyOverride")
    public ResultData<Boolean> emergencyOverride(@RequestBody java.util.Map<String, Object> input) {
        if (!input.containsKey("calllistid") || !input.containsKey("action")) {
            return ResultData.fail(500, "参数缺失");
        }
        Integer callListId = Integer.valueOf(input.get("calllistid").toString());
        String action = input.get("action").toString();

        boolean success = doctorCallService.emergencyOverride(callListId, action);
        return success ? ResultData.success(true, "操作成功") : ResultData.fail(500, "操作失败");
    }

    /**
     * 智能呼叫下一位（初复诊穿插）。
     * body：deptId 必填；officeRoomId/doctorId 可选（诊室粒度调度 + 接诊绑定）。
     */
    @PostMapping("/AutoCallNext")
    public ResultData<CallList> autoCallNext(@RequestBody java.util.Map<String, String> input) {
        String deptId = input.get("deptId");
        if (deptId == null || deptId.isEmpty()) {
            return ResultData.fail(500, "deptId 参数不能为空");
        }
        String officeRoomId = input.get("officeRoomId");
        String doctorId = input.get("doctorId");
        CallList result = doctorCallService.autoCallNext(deptId, officeRoomId, doctorId);
        if (result != null) {
            return ResultData.success(result, "智能获取下一位成功");
        }
        return ResultData.fail(404, "当前暂无等候患者");
    }

    /** 按父科室聚合子诊室分页队列（ClinicPatientList） */
    @PostMapping("/GetDeptCallList")
    public ResultData<java.util.Map<String, Object>> getDeptCallList(@RequestBody com.gocent.pdjh.dto.QueryInput input) {
        return ResultData.success(doctorCallService.getDeptCallList(input), "查询成功");
    }

    @PostMapping("/GetDoctorByDeptID")
    public ResultData<java.util.List<java.util.Map<String, String>>> getDoctorByDeptID(@RequestParam("DeptID") String deptId) {
        return ResultData.success(doctorCallService.getDoctorsByDeptForUi(deptId), "获取成功");
    }

    @PostMapping("/Register")
    public ResultData<Boolean> register(@RequestBody java.util.Map<String, Object> body) {
        boolean ok = doctorCallService.registerCall(body);
        return ok ? ResultData.success(true, "登记成功") : ResultData.fail(500, "登记失败");
    }

    @PostMapping("/RemoveCall")
    public ResultData<Boolean> removeCall(@RequestParam("id") Integer id) {
        boolean ok = doctorCallService.removeCall(id);
        return ok ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败");
    }

    @PostMapping("/EditorCall")
    public ResultData<Boolean> editorCall(@RequestBody java.util.Map<String, Object> body) {
        boolean ok = doctorCallService.editorCall(body);
        return ok ? ResultData.success(true, "修改成功") : ResultData.fail(500, "修改失败");
    }

    @PostMapping("/BatchRemove")
    public ResultData<Integer> batchRemove(@RequestBody java.util.Map<String, Object> body) {
        return ResultData.success(doctorCallService.batchRemove(body), "批量删除完成");
    }

    @PostMapping("/OneclickClear")
    public ResultData<Boolean> oneclickClear() {
        return doctorCallService.oneclickClear()
                ? ResultData.success(true, "清除成功")
                : ResultData.fail(500, "清除失败");
    }

    @PostMapping("/SortCall")
    public ResultData<Boolean> sortCall(@RequestBody java.util.Map<String, Object> body) {
        boolean ok = doctorCallService.sortCall(body);
        return ok ? ResultData.success(true, "调整成功") : ResultData.fail(500, "调整失败");
    }
}
