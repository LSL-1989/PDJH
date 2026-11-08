package com.gocent.pdjh.dto.clinic;

import lombok.Data;

@Data
public class ClinicDoctorVO {
    private String id;
    private String name;
    private String title;
    private String avatar;
    /** 医生简介（doctor.remarks） */
    private String remarks;
    private String specialty;
    private Integer isOnDuty;
}
