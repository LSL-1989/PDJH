package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.DocCallMsgInput;
import com.gocent.pdjh.entity.CallList;

import java.util.List;
import java.util.Map;

public interface DoctorCallService {
    
    /**
     * 获取当日等候/叫号队列。
     * @param deptId 科室 ID（officeRoomId 为空时按科室）
     * @param state 状态（0 等候等）
     * @param officeRoomId 可选诊室 ID；有值时仅返回该诊室队列（与门边屏对齐）
     */
    List<CallList> getCallList(String deptId, Integer state, String officeRoomId);

    // 新增：兼容后台管理大盘的综合分页与管系过滤检索
    Map<String, Object> getPaginatedCallList(com.gocent.pdjh.dto.QueryInput input);

    /**
     * 修改呼叫状态；state=1 时可顺带绑定诊室/医生。
     * @param callListId 排队 ID
     * @param state 目标状态
     * @param officeRoomId 可选诊室
     * @param doctorId 可选医生 ID
     */
    boolean editCallMsgState(Integer callListId, Integer state, String officeRoomId, String doctorId);

    /**
     * 发起呼叫，通过 WebSocket 广播消息，更新大屏和进行语音播报 
     * @param input 呼叫入参
     * @return 成功与否
     */
    boolean docCallMsg(DocCallMsgInput input);

    /**
     * 紧急插队或挂起延后 (US2 Phase 4)
     * @param callListId 排队ID
     * @param action "BOOST" 代表优先插队, "SUSPEND" 代表延后/挂起
     * @return 成功与否
     */
    boolean emergencyOverride(Integer callListId, String action);

    /**
     * 智能呼叫下一位（初复诊穿插 / 药房专线）。
     * @param deptId 科室 ID（必填）
     * @param officeRoomId 可选诊室；有值时仅从该诊室池调度，并写回 officeRoomId
     * @param doctorId 可选医生 ID；接诊时写回 doctorID
     * @return 选中患者，无人则 null
     */
    CallList autoCallNext(String deptId, String officeRoomId, String doctorId);

    /** 按父科室聚合子诊室队列（后台 ClinicPatientList） */
    Map<String, Object> getDeptCallList(com.gocent.pdjh.dto.QueryInput input);

    /** 诊室下医生下拉（兼容旧字段 UserName） */
    List<Map<String, String>> getDoctorsByDeptForUi(String deptId);

    boolean registerCall(Map<String, Object> body);

    boolean removeCall(Integer id);

    boolean editorCall(Map<String, Object> body);

    int batchRemove(Map<String, Object> body);

    boolean oneclickClear();

    boolean sortCall(Map<String, Object> body);
}
