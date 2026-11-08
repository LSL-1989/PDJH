package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 医生职称字典（dict_doctor_title）。
 * 消费者：医生职称维护页、医生档案职称下拉；doctor.title 存名称字符串，改名时由服务同步档案。
 */
@Data
@Entity
@Table(name = "dict_doctor_title")
public class DictDoctorTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 职称名称，与 doctor.title 一致 */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 排序，小的在前 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 1=启用（下拉可见） 0=停用 */
    @Column(name = "enabled")
    private Integer enabled = 1;
}
