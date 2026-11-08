package com.gocent.pdjh.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ScreensOutput {
    private String id;
    private String name;
    private Date createDate;
    private Integer state;
    private String callCode;
    private Integer callNum;
    private String callContent;
    private Integer maxNum;
    private String bulletin;
    private Integer scrollTime;
    private String subtitle;
    private Integer rate;
    private Integer dataMode;
    private String deptInfo;
    /** 物理诊室绑定信息 roomId|roomName;... */
    private String roomInfo;
    private String stateModel;
    private String isNum;
    private String isAudio;

    // 设备唯一识别码
    private String deviceCode;

    /** SCREEN / CLINIC */
    private String terminalType;

    /** 候诊模板路由 */
    private String templateRoute;

    /** 播报通道：VOICE | SCREEN | BOTH */
    private String voiceChannel;

    /** 本屏绑定的独立音响设备 ID */
    private List<String> voiceDeviceIds;

    /** 绑定音响摘要（后台列表展示） */
    private String voiceBoundSummary;

    // 扩展字段，用于大屏前端识别当前屏幕绑定的科室类别（如：1门诊，2医技）
    private Integer deptType;

    private java.util.Date startTime;
    private java.util.Date endTime;

    /** 大屏显示内容配置（sys_config.SCREEN_CFG_{id} 解析后的 JSON，供候诊区大屏渲染） */
    private Map<String, Object> contentConfig;

    /** 名称/呼叫代码/诊室绑定是否齐全（不含连接状态） */
    private Boolean configComplete;

    /** 终端 5 分钟内有过 Register/Resolve */
    private Boolean online;

    private java.util.Date lastSeenAt;
}
