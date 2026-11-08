package com.gocent.pdjh.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "gw_audit_log")
public class GwAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_code", length = 50)
    private String endpointCode;

    @Column(name = "connection_id", length = 36)
    private String connectionId;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}
