package com.gocent.pdjh.integration;

import com.gocent.pdjh.integration.model.HisPatientInfo;
import java.util.List;

/**
 * 医院信息系统 (HIS/LIS/PACS) 防腐层网关底层接口
 * 该接口抽象了排队系统向外部索取或推送数据的所有途径。
 */
public interface HospitalIntegrationService {

    /**
     * 根据患者卡号或挂号流水，实判断其今日合法身份与所属科室
     * @param patientCode 患者条码/身份证
     * @return 医院基础挂号信息DTO
     */
    HisPatientInfo getPatientRegistrationInfo(String patientCode);

    /**
     * 获取患者今日已缴费的体检明细清单
     * @param patientCode 患者条码
     * @return 项目代号列表 (如：ULTR01, BLD01)
     */
    List<String> getPaidExamItemCodes(String patientCode);

    /**
     * 当本系统内的排队状态扭转时 (如从等待进入就诊)，同步状态给院方总大屏或计费库
     * @param patientCode 患者
     * @param state 变更后的状态
     */
    void syncQueueStateToHis(String patientCode, int state);
}
