package com.gocent.pdjh.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "call_list")
public class CallList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "patient_code")
    @JsonProperty("PatientCode")
    private String patientCode;

    @Column(name = "patient_name")
    @JsonProperty("PatientName")
    private String patientName;

    @JsonProperty("sflbbigtype")
    private String sflbbigtype;

    @Column(name = "call_time")
    @JsonProperty("CallTime")
    private Date callTime;

    @JsonProperty("sflbmc")
    private String sflbmc;

    @JsonProperty("xh")
    private Integer xh;

    @JsonProperty("xhlb")
    private String xhlb;

    @Column(name = "show_no")
    @JsonProperty("ShowNo")
    private String showNo;

    @Column(name = "old_dept_id")
    @JsonProperty("OldDeptId")
    private String oldDeptId;

    @Column(name = "dept_id")
    @JsonProperty("DeptID")
    private String deptID;

    @Column(name = "out_patient_office")
    @JsonProperty("OutPatientOffice")
    private String outPatientOffice;

    @Column(name = "office_room_id", length = 64)
    @JsonProperty("OfficeRoomId")
    private String officeRoomId;

    @JsonProperty("Doctors")
    private String doctors;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("state")
    private Integer state;

    @JsonProperty("pym")
    private String pym;

    @Column(name = "call_date")
    @JsonProperty("CallDate")
    private Date callDate;

    @Column(name = "end_date")
    @JsonProperty("EndDate")
    private Date endDate;

    @Column(name = "doctor_id")
    @JsonProperty("DoctorID")
    private String doctorID;

    @JsonProperty("zcbz")
    private String zcbz;

    @Column(name = "is_forzen")
    @JsonProperty("IsForzen")
    private Integer isForzen;

    @JsonProperty("sex")
    private String sex;

    @Column(name = "age_str")
    @JsonProperty("ageStr")
    private String ageStr;

    @Column(name = "is_appointment")
    @JsonProperty("IsAppointment")
    private Integer isAppointment;

    @Column(name = "appointment_start_time")
    @JsonProperty("AppointmentStartTime")
    private String appointmentStartTime;

    @Column(name = "appointment_end_time")
    @JsonProperty("AppointmentEndTime")
    private String appointmentEndTime;

    @Column(name = "start_time")
    @JsonProperty("StartTime")
    private Date startTime;

    @Column(name = "triage_state")
    @JsonProperty("TriageState")
    private String triageState;

    @Column(name = "call_count")
    @JsonProperty("CallCount")
    private Integer callCount;

    @Column(name = "check_in_time")
    @JsonProperty("CheckInTime")
    private Date checkInTime;

    @Column(name = "source_type")
    @JsonProperty("SourceType")
    private String sourceType;

    /** 体检当前项目名（不落库，GetDocCallList 附带） */
    @jakarta.persistence.Transient
    @JsonProperty("examItemName")
    private String examItemName;

    /** 体检下一站摘要（不落库） */
    @jakarta.persistence.Transient
    @JsonProperty("examNextHint")
    private String examNextHint;
}
