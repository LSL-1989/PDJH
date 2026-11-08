package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.enums.PatientFlowEvent;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 患者全流程操作留痕：各端写同一张表，后台列表 + 时间轴查询
 */
public interface PatientFlowLogService {

    /**
     * 异步记录一条流程事件（失败仅打日志，不影响主业务）
     */
    void record(CallList call, PatientFlowEvent event, String operatorSource,
                String operatorNo, String operatorName, String remark, Object payload);

    /** 无 CallList 实体时按字段记录 */
    void recordRaw(Integer callListId, String patientCode, String patientName, String showNo,
                   String deptId, PatientFlowEvent event, String operatorSource,
                   String operatorNo, String operatorName, String remark, Object payload);

    /**
     * 分页列表
     * @return data + pageInfo.totalCount
     */
    Map<String, Object> queryPage(int pageIndex, int pageSize, String keyword, String eventType,
                                  String patientCode, Integer callListId, Date startTime, Date endTime);

    /** 按排队单 ID 时间轴（升序） */
    List<Map<String, Object>> timelineByCallListId(Integer callListId);

    /** 按患者编号时间轴（升序，可加日期范围） */
    List<Map<String, Object>> timelineByPatientCode(String patientCode, Date startTime, Date endTime);
}
