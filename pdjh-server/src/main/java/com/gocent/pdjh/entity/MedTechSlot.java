package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

/**
 * 医技科室预约时段配额
 * 用于超声科 / 放射科（CT/MRI）等需要分时段限流的科室
 */
@Data
@Entity
@Table(name = "med_tech_slot", indexes = {
    @Index(name = "idx_slot_dept_date", columnList = "dept_id,slot_date")
})
public class MedTechSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属科室 ID（对应 department.id） */
    @Column(name = "dept_id", length = 36, nullable = false)
    private String deptId;

    /** 预约日期 */
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    /** 时段起始（如 08:00, 08:30, 09:00） */
    @Column(name = "time_slot", length = 10, nullable = false)
    private String timeSlot;

    /** 该时段总配额上限 */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 2")
    private int capacity = 2;

    /** 已预约数量（实时更新） */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int booked = 0;

    /** 是否可用（0=正常, 1=维护/暂停预约） */
    @Column(name = "is_disabled", columnDefinition = "INT DEFAULT 0")
    private Integer isDisabled = 0;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    /** 备注（如特殊节假日说明） */
    @Column(length = 200)
    private String remark;
}
