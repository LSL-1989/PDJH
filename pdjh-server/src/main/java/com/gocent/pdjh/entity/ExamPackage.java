package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "exam_package")
public class ExamPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "target_group", length = 100)
    private String targetGroup;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 32)
    private String color;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean enabled = true;

    @Column(length = 500)
    private String remark;
}
