package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "dict_exam_item")
public class DictExamItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 检查项目名称（如 腹部B超，抽血）
    private String itemName;

    // 该项目关联的科室 (DeptID)
    private String associatedDeptId;

    // 是否需要空腹 (1=是, 0=否)
    private Integer isFasting;

    // 性别限制 (0=不限, 1=男性专享, 2=女性专享)
    private Integer genderLimit;

    // 平均耗时 (分钟), 用于预估排队时间
    private Integer avgTimeMinutes;
    
    // 是否启用
    private Integer isEnabled;
    
    // 前置项目依赖项的 ID 集合，逗号分隔 (如 "1,5")
    @Column(name = "depends_on_ids", length = 255)
    private String dependsOnIds;
}
