package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 患者就诊/取药全流程操作留痕（签到→叫号→完成/过号/复诊及药房链路）
 * 消费者：后台患者流程日志列表与时间轴抽屉；与 nurse_action_log / sys_action_log 互补，本表按患者轨迹查询
 */
@Data
@Entity
@Table(name = "patient_flow_log", indexes = {
        @Index(name = "idx_pfl_call_list", columnList = "call_list_id"),
        @Index(name = "idx_pfl_patient_time", columnList = "patient_code,create_time"),
        @Index(name = "idx_pfl_event_time", columnList = "event_type,create_time"),
        @Index(name = "idx_pfl_create_time", columnList = "create_time")
})
public class PatientFlowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_list_id")
    private Integer callListId;

    @Column(name = "patient_code", length = 64)
    private String patientCode;

    @Column(name = "patient_name", length = 50)
    private String patientName;

    @Column(name = "show_no", length = 50)
    private String showNo;

    @Column(name = "dept_id", length = 64)
    private String deptId;

    /** 事件码，见 {@link com.gocent.pdjh.enums.PatientFlowEvent} */
    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    /** 事件中文名（冗余便于列表展示） */
    @Column(name = "event_label", length = 50)
    private String eventLabel;

    /** 操作来源：TERMINAL / DOCTOR / PHARMACY / NURSE / SYS */
    @Column(name = "operator_source", length = 20)
    private String operatorSource;

    @Column(name = "operator_no", length = 50)
    private String operatorNo;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(length = 500)
    private String remark;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = new Date();
        }
    }
}
