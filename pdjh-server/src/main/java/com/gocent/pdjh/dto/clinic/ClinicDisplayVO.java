package com.gocent.pdjh.dto.clinic;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClinicDisplayVO {
    /** 全院医院名称，来自 sys_config.HospitalName */
    private String hospitalName;
    /** 医院 Logo 地址，来自 sys_config.HospitalLogo */
    private String hospitalLogo;
    private ClinicScreenVO screen;
    private ClinicRoomVO room;
    private List<ClinicDoctorVO> doctors = new ArrayList<>();
    private List<ClinicPatientRowVO> currentPatients = new ArrayList<>();
    private List<ClinicPatientRowVO> waitingPatients = new ArrayList<>();
}
