package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class EnqueueRequest {
    // 患者标识
    private String patientCode;
    private String patientName;
    private String sex;
    private String ageStr;
    
    // 排队位置信息
    private String deptID; // 科室ID / 药房ID / 检验科室ID
    /** 物理诊室 ID（office_room.id），区域签到校验依据 */
    private String officeRoomId;
    private String queueType; // 队列类型说明 (例如 "挂号", "取药", "抽血")
    
    // 扩展信息
    private String showNo; // 显示的号码，如"A001"
    private Integer priority; // 优先级 / 序号
    private String doctors; // 分配的医生名称或窗口名称（如不指定可为空）
    private String doctorID; // 医生ID 或 窗口ID (如需要直接指定给某人/某窗口)
    
    // 是否需要签到配置 (true=进入待签到池 state=7; false=直接排队 state=0)
    private Boolean requireSignIn;

    // 预约机制相关扩展
    private Integer isAppointment; // 是否为预约号源 (1代表是，0或空代表现场号)
    private String appointmentStartTime; // 预约开始时段，如 "09:30:00"
    private String appointmentEndTime; // 预约结束时段，如 "10:00:00"

    // 患者来源类型（P1-2）
    // CLINIC=普通门诊 | OUTPATIENT_EXAM=门诊医技 | HEALTH_CHECKUP=体检导检 | PHARMACY=药房
    private String sourceType;

    /** 药品或收费大类。药房分窗用，空则由签到侧走最短队列。 */
    private String sflbbigtype;
}
