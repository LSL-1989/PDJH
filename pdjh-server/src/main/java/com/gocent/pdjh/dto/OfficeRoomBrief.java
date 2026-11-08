package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class OfficeRoomBrief {
    private String id;
    private String name;
    private String floor;
    private String roomNo;
    private String deptId;
    private String locationLabel;
    /** OUTPATIENT / PHARMACY / LAB / EXAM 等，供签到机推断场景 */
    private String businessType;
}
