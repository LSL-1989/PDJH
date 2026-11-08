package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 护士站干预审计日志
 * 消费者：NurseDesk/GetActionLogs、后台审计追溯
 */
@Data
@Entity
@Table(name = "nurse_action_log", indexes = {
        @Index(name = "idx_nurse_log_zone_time", columnList = "zone_id,create_time"),
        @Index(name = "idx_nurse_log_call_list", columnList = "call_list_id")
})
public class NurseActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", length = 36)
    private String zoneId;

    @Column(name = "call_list_id")
    private Integer callListId;

    @Column(name = "dept_id", length = 36)
    private String deptId;

    @Column(name = "operator_user_no", length = 50)
    private String operatorUserNo;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "operator_role", length = 50)
    private String operatorRole;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

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
