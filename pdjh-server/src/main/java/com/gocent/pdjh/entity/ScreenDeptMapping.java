package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "screen_dept_mapping") // 关联大屏与科室
public class ScreenDeptMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "screen_id", length = 36)
    private String screenId;

    @Column(name = "dept_id", length = 36)
    private String deptId;
}
