package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "exam_record_detail")
public class ExamRecordDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "patient_code")
    private String patientCode;

    @Column(name = "exam_batch_no")
    private String examBatchNo;

    @Column(name = "item_id")
    private Integer itemId;

    private Integer state;

    @Column(name = "enqueue_time")
    private Date enqueueTime;

    @Column(name = "complete_time")
    private Date completeTime;
}
