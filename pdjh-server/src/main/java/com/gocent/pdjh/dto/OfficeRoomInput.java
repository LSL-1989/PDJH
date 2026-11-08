package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class OfficeRoomInput {
    private String id;
    private String name;
    private String deptId;
    private String floor;
    private String roomNo;
    private String doctorId;
    private String screenId;
    private String voiceDeviceId;
    private Integer state;
    /** 专有业务场景；空则跟随所属科室 businessType */
    private String businessType;
}
