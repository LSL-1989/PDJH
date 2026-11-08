package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 医生-科室多对多；无主科室概念。
 * 消费者：DoctorProfile 多选、医生站按所属科室并集选诊室、排班筛选。
 */
@Data
@Entity
@Table(name = "doctor_dept", indexes = {
        @Index(name = "idx_doctor_dept_doctor", columnList = "doctor_id"),
        @Index(name = "idx_doctor_dept_dept", columnList = "dept_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_dept", columnNames = {"doctor_id", "dept_id"})
})
public class DoctorDept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "dept_id", length = 64, nullable = false)
    private String deptId;
}
