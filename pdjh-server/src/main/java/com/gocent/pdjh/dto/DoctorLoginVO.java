package com.gocent.pdjh.dto;

import com.gocent.pdjh.entity.OfficeRoom;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 叫号器登录成功返回（不含密码）
 * 消费者：HJ叫号器 WPF（门诊/药房/检验/医技体检按 stationMode 分流界面）
 */
@Data
public class DoctorLoginVO {
    private String doctorId;
    private String workNo;
    private String name;
    private String phone;
    private String deptId;
    private String title;
    /** 1=需强制改密（首次或重置后） */
    private Integer mustChangePassword;
    /**
     * 叫号器模式：CLINIC=门诊诊室，PHARMACY=药房窗口，LAB=检验窗口，EXAM=医技/体检检查室
     * 优先取默认/上岗诊室 office_room.business_type，否则回退科室
     */
    private String stationMode;
    /** 科室 businessType 原文，便于客户端展示 */
    private String businessType;
    /** office_room.doctor_id = 本医生 的默认绑定诊室；无则 null */
    private OfficeRoom defaultRoom;
    /** 本科室开放诊室，供切换 */
    private List<OfficeRoom> rooms = new ArrayList<>();
}
