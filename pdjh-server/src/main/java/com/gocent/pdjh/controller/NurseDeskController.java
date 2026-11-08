package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.NurseDeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 分诊台护士工作站 API
 * 消费者：HJ护士工作站 src/api/nurseDesk.js
 */
@RestController
@RequestMapping("/NurseDesk")
@RequiredArgsConstructor
public class NurseDeskController {

    private final NurseDeskService nurseDeskService;

    /** 登录后会话：诊区、科室、诊室列表；入参 operatorUserNo + token，可选 zoneId */
    @PostMapping("/GetSession")
    public ResultData<Map<String, Object>> getSession(@RequestBody Map<String, String> body) {
        return ResultData.success(
                nurseDeskService.getSession(
                        body.get("operatorUserNo"), body.get("token"), body.get("zoneId")),
                "获取会话成功");
    }

    /**
     * 登录页当班诊区选项：当日排班 / 常驻 / 授权集合。
     * 与 GetSession 分开，避免未选定诊区时拉全量诊室。
     */
    @PostMapping("/ListDutyZones")
    public ResultData<Map<String, Object>> listDutyZones(@RequestBody Map<String, String> body) {
        return ResultData.success(
                nurseDeskService.listDutyZones(body.get("operatorUserNo"), body.get("token")),
                "获取当班诊区成功");
    }

    /**
     * 当班中途切换诊区；body：zoneId，可选 reasonCode/remark。写 SWITCH_ZONE 审计。
     */
    @PostMapping("/SwitchZone")
    public ResultData<Map<String, Object>> switchZone(@RequestBody Map<String, Object> body) {
        return ResultData.success(
                nurseDeskService.switchZone(
                        stringVal(body.get("operatorUserNo")),
                        stringVal(body.get("token")),
                        body),
                "已切换诊区");
    }

    @PostMapping("/GetOverview")
    public ResultData<Map<String, Object>> getOverview(@RequestBody Map<String, String> body) {
        return ResultData.success(
                nurseDeskService.getOverview(body.get("operatorUserNo"), body.get("token")),
                "获取概览成功");
    }

    @PostMapping("/GetAlerts")
    public ResultData<List<Map<String, Object>>> getAlerts(@RequestBody Map<String, String> body) {
        return ResultData.success(
                nurseDeskService.getAlerts(body.get("operatorUserNo"), body.get("token")),
                "获取告警成功");
    }

    @PostMapping("/SearchPatient")
    public ResultData<List<Map<String, Object>>> searchPatient(@RequestBody Map<String, Object> body) {
        return ResultData.success(
                nurseDeskService.searchPatient(
                        stringVal(body.get("operatorUserNo")),
                        stringVal(body.get("token")),
                        stringVal(body.get("keyword"))),
                "搜索成功");
    }

    @PostMapping("/ManualSignIn")
    public ResultData<Void> manualSignIn(@RequestBody Map<String, Object> body) {
        nurseDeskService.manualSignIn(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "签到成功");
    }

    @PostMapping("/BindOfficeRoom")
    public ResultData<Void> bindOfficeRoom(@RequestBody Map<String, Object> body) {
        nurseDeskService.bindOfficeRoom(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "绑定诊室成功");
    }

    @PostMapping("/Revive")
    public ResultData<Void> revive(@RequestBody Map<String, Object> body) {
        nurseDeskService.revive(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "复活成功");
    }

    @PostMapping("/ManualCutIn")
    public ResultData<Void> manualCutIn(@RequestBody Map<String, Object> body) {
        nurseDeskService.manualCutIn(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "插队成功");
    }

    @PostMapping("/ForceSwap")
    public ResultData<Void> forceSwap(@RequestBody Map<String, Object> body) {
        nurseDeskService.forceSwap(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "转诊成功");
    }

    @PostMapping("/SetFrozen")
    public ResultData<Void> setFrozen(@RequestBody Map<String, Object> body) {
        nurseDeskService.setFrozen(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "操作成功");
    }

    @PostMapping("/MarkHisRefunded")
    public ResultData<Void> markHisRefunded(@RequestBody Map<String, Object> body) {
        nurseDeskService.markHisRefunded(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "已同步作废队列");
    }

    @PostMapping("/SuspendDept")
    public ResultData<Void> suspendDept(@RequestBody Map<String, Object> body) {
        nurseDeskService.suspendDept(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "科室已停诊");
    }

    @PostMapping("/ResumeDept")
    public ResultData<Void> resumeDept(@RequestBody Map<String, Object> body) {
        nurseDeskService.resumeDept(
                stringVal(body.get("operatorUserNo")),
                stringVal(body.get("token")),
                body);
        return ResultData.success(null, "科室已恢复出诊");
    }

    /**
     * 诊室批量换房；body：fromRoomId, toRoomId, moveDoctor?, reasonCode, remark, operatorUserNo, token
     */
    @PostMapping("/MigrateRoom")
    public ResultData<Map<String, Object>> migrateRoom(@RequestBody Map<String, Object> body) {
        try {
            return ResultData.success(
                    nurseDeskService.migrateRoom(
                            stringVal(body.get("operatorUserNo")),
                            stringVal(body.get("token")),
                            body),
                    "换房迁移完成");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    @PostMapping("/GetActionLogs")
    public ResultData<Map<String, Object>> getActionLogs(@RequestBody Map<String, Object> body) {
        int page = body.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = body.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return ResultData.success(
                nurseDeskService.getActionLogs(
                        stringVal(body.get("operatorUserNo")),
                        stringVal(body.get("token")),
                        page,
                        pageSize),
                "查询审计日志成功");
    }

    private static String stringVal(Object v) {
        return v == null ? null : v.toString();
    }
}
