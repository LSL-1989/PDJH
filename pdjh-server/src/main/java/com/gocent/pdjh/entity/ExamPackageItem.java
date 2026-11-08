package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 体检套餐与检查项目的关联表（P1-5a）
 * 一个套餐可以关联多个检查项目（many-to-many with sort）
 */
@Data
@Entity
@Table(name = "exam_package_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"package_id", "item_id"}))
public class ExamPackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 套餐ID（对应 sys_config 中 EXAM_PACKAGE_* 的配置 key，或数据库 exam_package 表）*/
    @Column(name = "package_id", nullable = false)
    private Long packageId;

    /** 检查项目 ID（关联 dict_exam_item.id）*/
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    /** 在套餐内的排序（越小越优先） */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 该项目在此套餐中的备注（如：可选项目）*/
    @Column(name = "remark", length = 200)
    private String remark;
}
