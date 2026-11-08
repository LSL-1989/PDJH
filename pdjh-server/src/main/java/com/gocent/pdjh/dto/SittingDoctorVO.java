package com.gocent.pdjh.dto;

import lombok.Data;

/**
 * 当前已登录叫号器、可供签到选择的坐诊医生
 */
@Data
public class SittingDoctorVO {
    private String doctorId;
    private String workNo;
    private String name;
    private String title;
    private String deptId;
    private String deptName;
    private String officeRoomId;
    private String officeRoomName;
    private String avatar;
}
