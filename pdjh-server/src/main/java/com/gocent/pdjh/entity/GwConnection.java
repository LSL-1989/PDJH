package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "gw_connection")
public class GwConnection {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String type; // HTTP, JDBC, WEBHOOK

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String password;

    /** 仅响应使用，不入库。true 表示库里已有密码。 */
    @Transient
    private Boolean passwordSet;

    @Column(name = "driver_class", length = 100)
    private String driverClass; // for JDBC

    /** WEBHOOK 来源 IP，逗号分隔；空或含 * 表示不限制。 */
    @Column(name = "allow_ips", length = 500)
    private String allowIps;

    private Integer status = 1;

    @Column(name = "create_time")
    private Date createTime;
    
    @Column(name = "update_time")
    private Date updateTime;
}
