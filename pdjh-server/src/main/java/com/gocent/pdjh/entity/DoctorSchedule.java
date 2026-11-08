package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "doctor_schedule", indexes = {
        @Index(name = "idx_doc_sched_week", columnList = "doctor_id,schedule_date")
})
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", length = 64, nullable = false)
    private String doctorId;

    @Column(name = "dept_id", length = 64)
    private String deptId;

    /** 可选：当日坐诊物理诊室；删除诊室时据此校验 */
    @Column(name = "office_room_id", length = 64)
    private String officeRoomId;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    /** 班次编码，对应 dict_shift_type.shift_code。同一医生同一天可有多条（上午/下午诊室可不同） */
    @Column(name = "shift_type", length = 16, nullable = false)
    private String shiftType;

    /**
     * 1=该午别停诊。不填诊室和号源，remark 为缘由。
     * 与班次编码 OFF（主任台整天临时停诊）不同：这里上午可出诊、下午可停诊。
     */
    @Column(name = "stop_flag")
    private Integer stopFlag = 0;

    @Column(name = "max_quota")
    private Integer maxQuota;

    @Column(length = 200)
    private String remark;
}
