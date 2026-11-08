package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 后台首页待处理事项：科室冲突、医生未绑科室等。
 * 消费者：Dashboard 待处理栏、科室/医生维护页跳转处理。
 */
@Data
@Entity
@Table(name = "pending_issue", indexes = {
        @Index(name = "idx_pending_status_type", columnList = "status,issue_type")
})
public class PendingIssue {

    public static final String TYPE_DEPT_ID_CONFLICT = "DEPT_ID_CONFLICT";
    public static final String TYPE_DEPT_NAME_CONFLICT = "DEPT_NAME_CONFLICT";
    public static final String TYPE_DOCTOR_NO_DEPT = "DOCTOR_NO_DEPT";
    public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    public static final int STATUS_OPEN = 0;
    public static final int STATUS_RESOLVED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_type", length = 40, nullable = false)
    private String issueType;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 500)
    private String detail;

    /** 关联业务主键（科室 ID / 医生 ID 等） */
    @Column(name = "ref_id", length = 64)
    private String refId;

    @Column(name = "payload_json", length = 2000)
    private String payloadJson;

    /** 0=待处理 1=已处理 */
    @Column(nullable = false)
    private Integer status = STATUS_OPEN;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "resolve_date")
    private Date resolveDate;
}
