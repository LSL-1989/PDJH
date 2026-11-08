package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/**
 * 护士当班排班（T-19）：按工号+日期确定会话诊区，与医生 doctor_schedule 对称。
 * 消费者：NurseDesk 会话解析、后台护士当班排班页。
 */
@Data
@Entity
@Table(name = "nurse_schedule", indexes = {
        @Index(name = "idx_nurse_sched_user_date", columnList = "user_no,schedule_date")
})
public class NurseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_no", length = 50, nullable = false)
    private String userNo;

    /** 当班诊区（或叶子科室，会话解析时会归到诊区节点） */
    @Column(name = "dept_id", length = 64, nullable = false)
    private String deptId;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    /** AM / PM / FULL */
    @Column(name = "shift_type", length = 16, nullable = false)
    private String shiftType;

    @Column(name = "create_time")
    private Date createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Date();
        }
    }
}
