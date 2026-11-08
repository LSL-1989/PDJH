package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 诊室（物理诊室）实体
 * 一个科室可有多个物理诊室，每个诊室对应一名当前坐诊医生
 */
@Data
@Entity
@Table(name = "office_room")
public class OfficeRoom {

    @Id
    @Column(length = 64)
    private String id;

    /** 诊室显示名称（如"内科一诊室"） */
    @Column(length = 100)
    private String name;

    /** 所属科室ID */
    @Column(name = "dept_id", length = 64)
    private String deptId;

    /** 楼层（如"3F"） */
    @Column(length = 20)
    private String floor;

    /** 房间号（如"301"） */
    @Column(name = "room_no", length = 20)
    private String roomNo;

    /** 当前坐诊医生ID */
    @Column(name = "doctor_id", length = 64)
    private String doctorId;

    /** 关联的大屏ID */
    @Column(name = "screen_id", length = 64)
    private String screenId;

    /** 关联的发声设备ID(如智能音箱IP) */
    @Column(name = "voice_device_id", length = 64)
    private String voiceDeviceId;

    /** 诊室状态：1=开放 0=关闭 */
    @Column
    private Integer state = 1;

    /** 业务分类：OUTPATIENT(门诊), PHARMACY(药房), LAB(检验), EXAM(医技/体检) */
    @Column(name = "business_type", length = 50)
    private String businessType;
}
