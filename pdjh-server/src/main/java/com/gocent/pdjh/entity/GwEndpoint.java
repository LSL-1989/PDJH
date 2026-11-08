package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "gw_endpoint")
public class GwEndpoint {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "connection_id", length = 36)
    private String connectionId;

    @Column(name = "endpoint_code", length = 50)
    private String endpointCode;

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String path; // URI path or SQL query string

    @Column(length = 20)
    private String method; // GET, POST, POLLING

    @Column(name = "cron_expression", length = 50)
    private String cronExpression;

    @Column(name = "env_mode")
    private Integer envMode = 0; // 0: PROD, 1: MOCK

    @Column(name = "mock_path", length = 500)
    private String mockPath;

    private Integer status = 1;

    @Column(name = "create_time")
    private Date createTime;
    
    @Column(name = "update_time")
    private Date updateTime;
}
