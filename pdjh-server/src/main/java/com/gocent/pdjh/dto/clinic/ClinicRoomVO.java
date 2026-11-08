package com.gocent.pdjh.dto.clinic;

import lombok.Data;

@Data
public class ClinicRoomVO {
    private String id;
    private String name;
    private String deptId;
    private String deptName;
    private String floor;
    private String roomNo;
}
