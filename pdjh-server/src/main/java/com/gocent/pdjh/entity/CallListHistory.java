package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "call_list_history")
public class CallListHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "patient_code")
    private String patientCode;

    @Column(name = "patient_name")
    private String patientName;

    private String sflbbigtype;

    @Column(name = "call_time")
    private Date callTime;

    private String sflbmc;
    private Integer xh;
    private String xhlb;

    @Column(name = "show_no")
    private String showNo;

    @Column(name = "old_dept_id")
    private String oldDeptId;

    @Column(name = "dept_id")
    private String deptID;

    @Column(name = "out_patient_office")
    private String outPatientOffice;

    private String doctors;
    private String remark;
    private Integer state;
    private String pym;

    @Column(name = "call_date")
    private Date callDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "doctor_id")
    private String doctorID;

    private String zcbz;

    @Column(name = "is_forzen")
    private Integer isForzen;

    private String sex;

    @Column(name = "age_str")
    private String ageStr;

    @Column(name = "is_appointment")
    private Integer isAppointment;

    @Column(name = "appointment_start_time")
    private String appointmentStartTime;

    @Column(name = "appointment_end_time")
    private String appointmentEndTime;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "triage_state")
    private String triageState;

    @Column(name = "call_count")
    private Integer callCount;

    @Column(name = "check_in_time")
    private Date checkInTime;

    @Column(name = "source_type")
    private String sourceType;
}
