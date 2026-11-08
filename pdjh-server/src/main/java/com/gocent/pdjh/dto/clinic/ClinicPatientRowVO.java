package com.gocent.pdjh.dto.clinic;

import lombok.Data;

@Data
public class ClinicPatientRowVO {
    private String doctorId;
    private String showNo;
    private Integer xh;
    private String patientName;
    /** 分诊状态：REVISIT 复诊，WAITING/null 初诊；供门边屏「初·09·张*花」展示 */
    private String triageState;
    private Integer state;
}
