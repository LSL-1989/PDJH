package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "department") // 旧系统中的表名可能大写或有前缀，目前默认 `department`，之后如果发现数据库有别名再改库名
public class Department {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(length = 100)
    private String name;
    
    private Integer type;
    
    // 业务分类：OUTPATIENT(门诊), PHARMACY(药房), LAB(检验), EXAM(医技/体检)
    @Column(name = "business_type", length = 50)
    private String businessType;
    
    private Integer state;
    
    @Column(name = "father_id", length = 36) // C#中是 FatherID
    private String fatherID;
    
    @Column(length = 50)
    private String queue;
    
    // 危机管控：是否被挂起/宕机(1=挂起, 0=正常)
    @Column(name = "is_suspended", columnDefinition = "INT DEFAULT 0")
    private Integer isSuspended = 0;

    // 医技科室子类型：LAB(检验科), ULTRASOUND(超声科), RADIOLOGY(放射科), ECG(心电图室)
    // 仅 businessType=LAB 或 EXAM 的科室需要填写此字段
    @Column(name = "med_tech_sub_type", length = 20)
    private String medTechSubType;

}
