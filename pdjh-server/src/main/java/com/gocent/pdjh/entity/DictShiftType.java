package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 医生出诊班次字典（dict_shift_type）。
 * 消费者：系统管理「排班类型」页、出诊排班甘特、医生管理查看排班。
 * 与护士当班班次不合并：护士仍使用固定 AM/PM/FULL。
 * doctor_schedule.shift_type 存本表 shift_code。
 */
@Data
@Entity
@Table(name = "dict_shift_type")
public class DictShiftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 班次编码，创建后不可改，避免已排班记录对不上 */
    @Column(name = "shift_code", nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 开始时刻 HH:mm；停诊类可空 */
    @Column(name = "start_time", length = 5)
    private String startTime;

    /** 结束时刻 HH:mm；停诊类可空 */
    @Column(name = "end_time", length = 5)
    private String endTime;

    /** 甘特色标：am / pm / full / off / default */
    @Column(name = "tag_style", length = 16)
    private String tagStyle;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 1=启用（新增排班可选） 0=停用 */
    @Column(name = "enabled")
    private Integer enabled = 1;

    /** 1=停诊类，与同日其他班次互斥 */
    @Column(name = "exclusive_flag")
    private Integer exclusiveFlag = 0;
}
