package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.util.Date;

/**
 * 登录审计（users 统一认证后各端共用）。
 * 消费者：LoginLogService；叫号器 Doctor/Login、后台 User/LoginUser。
 */
@Data
@Entity
@Table(name = "login_log", indexes = {
        @Index(name = "idx_login_log_user", columnList = "user_no"),
        @Index(name = "idx_login_log_time", columnList = "login_time")
})
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_no", length = 50)
    private String userNo;

    /** 登录场景：ADMIN / DOCTOR / NURSE / PHARMACY / LAB / EXAM */
    @Column(length = 20)
    private String scene;

    @Column(name = "login_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date loginTime;

    @Column(length = 64)
    private String ip;

    /** 1 成功 0 失败 */
    private Integer success;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (loginTime == null) {
            loginTime = new Date();
        }
    }
}
