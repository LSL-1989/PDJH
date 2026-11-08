package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "sign_device")
public class SignDevice {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "device_code", length = 100, unique = true)
    private String deviceCode;

    @Column(length = 100)
    private String name;

    @Column(length = 200)
    private String title;

    @Column(length = 200)
    private String location;

    @Column(name = "show_text", length = 500)
    private String showText;

    private Integer status;

    @Column(name = "last_seen_at")
    private Date lastSeenAt;

    @Column(name = "create_date")
    private Date createDate;
}
