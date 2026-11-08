package com.gocent.pdjh.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

/**
 * 核心系统动作日志（用于追溯核心排队叫号/大屏广播/外部推送等行为）
 */
@Data
@Entity
@Table(name = "sys_action_log", indexes = {
    @Index(name = "idx_syslog_type", columnList = "log_type"),
    @Index(name = "idx_syslog_time", columnList = "create_time")
})
public class SystemActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 日志分类: VOICE_BROADCAST, DOCTOR_CALL, MSG_PUSH, SYS_MAINTENANCE 等
     */
    @Column(name = "log_type", length = 30)
    private String logType;

    /**
     * 触发者: "SYS" 或 具体医生名称、操作员姓名
     */
    @Column(length = 100)
    private String operator;

    /**
     * 人类可读短描述: e.g., "张医生叫号: A012"
     */
    @Column(name = "action_desc", length = 500)
    private String actionDesc;

    /**
     * 原始报文或核心快照 JSON
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * 是否执行成功
     */
    @Column(name = "is_success")
    private Boolean isSuccess;

    /**
     * 异常堆栈信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
