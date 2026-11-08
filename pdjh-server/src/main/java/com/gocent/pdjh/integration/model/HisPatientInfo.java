package com.gocent.pdjh.integration.model;

import lombok.Data;

/**
 * 院内系统交互标准数据传输对象 (DTO)
 */
@Data
public class HisPatientInfo {
    private String patientCode;
    private String patientName;
    private String deptId;
    
    // 是否为预约号（1-是，0-否）
    private Integer isAppointment;
    
    // 门诊/体检 内部唯一流水号
    private String orderNo;
    
    // 是否为特权优先人群 (如高龄或伤残)
    private Boolean isSpecialCare;
}
