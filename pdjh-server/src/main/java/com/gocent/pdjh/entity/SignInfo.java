package com.gocent.pdjh.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "sign_info")
public class SignInfo {
    @Id
    private String gid;
    
    private String code;
    private String name;
    private String title;
    private String deptInfo;
    private Integer isQueue;
    private String signView;
    private String dataSource;
    private String dataSourceType;
    private String showText;
    private String checkType;
    private Integer cardNum;
    private Integer isShowInfo;
    private Integer status;
    private Date createDate;
}
