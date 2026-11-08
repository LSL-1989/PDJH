package com.gocent.pdjh.service;

import java.util.List;
import java.util.Map;

/**
 * 分诊台护士工作站聚合服务
 * 消费者：NurseDeskController、HJ护士工作站
 */
public interface NurseDeskService {

    /**
     * 会话诊区、科室、诊室列表。
     * @param requestedZoneId 登录选择的诊区；有当日排班时忽略。空则排班 → 常驻回退。
     */
    Map<String, Object> getSession(String operatorUserNo, String token, String requestedZoneId);

    /**
     * 登录页用：当日排班、常驻诊区、授权诊区列表（不含诊室明细）。
     * 与 GetSession 分开，避免未选定诊区时拉全量诊室。
     */
    Map<String, Object> listDutyZones(String operatorUserNo, String token);

    /**
     * 当班中途切换诊区（须在授权集合内），写 nurse_action_log SWITCH_ZONE，并落成今日排班。
     * 入参 body.zoneId；出参同 GetSession。
     */
    Map<String, Object> switchZone(String operatorUserNo, String token, Map<String, Object> body);

    Map<String, Object> getOverview(String operatorUserNo, String token);

    List<Map<String, Object>> getAlerts(String operatorUserNo, String token);

    List<Map<String, Object>> searchPatient(String operatorUserNo, String token, String keyword);

    void manualSignIn(String operatorUserNo, String token, Map<String, Object> body);

    void bindOfficeRoom(String operatorUserNo, String token, Map<String, Object> body);

    void revive(String operatorUserNo, String token, Map<String, Object> body);

    void manualCutIn(String operatorUserNo, String token, Map<String, Object> body);

    void forceSwap(String operatorUserNo, String token, Map<String, Object> body);

    void setFrozen(String operatorUserNo, String token, Map<String, Object> body);

    void markHisRefunded(String operatorUserNo, String token, Map<String, Object> body);

    void suspendDept(String operatorUserNo, String token, Map<String, Object> body);

    void resumeDept(String operatorUserNo, String token, Map<String, Object> body);

    /**
     * 诊室故障批量换房：将 fromRoom 当日候诊/接诊患者迁到 toRoom，可选迁移坐诊医生。
     */
    Map<String, Object> migrateRoom(String operatorUserNo, String token, Map<String, Object> body);

    Map<String, Object> getActionLogs(String operatorUserNo, String token, int page, int pageSize);
}
