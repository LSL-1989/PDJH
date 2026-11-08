package com.gocent.pdjh.dto.clinic;

import lombok.Data;

@Data
public class ClinicScreenVO {
    private String id;
    private Integer state;
    private String bulletin;
    private Integer scrollIntervalSec;
    private String callCode;
    /** 患者姓名是否脱敏展示 */
    private Boolean nameDesensitize;
    /** 是否展示等候列表区域 */
    private Boolean showWaitList;
}
