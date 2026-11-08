package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.List;

@Data
public class SignDeviceConfigOutput {
    private String deviceCode;
    private String name;
    private String title;
    private String location;
    private String showText;
    private List<OfficeRoomBrief> boundRooms;
    private String boundRoomSummary;
    /** CLINIC | PHARMACY | LAB，由绑定诊室 businessType 推断 */
    private String scene;
    /** 全院医院名称，来自 sys_config.HospitalName（与门边屏一致） */
    private String hospitalName;
    /** 医院 Logo 地址，来自 sys_config.HospitalLogo（与门边屏一致） */
    private String hospitalLogo;
}
