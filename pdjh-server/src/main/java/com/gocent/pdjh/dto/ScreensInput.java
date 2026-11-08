package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScreensInput {
    private String id; // 原有是 Guid 这里按 String 接收简化
    private String name;
    private String state;
    private String callCode;
    private Integer callNum;
    private String callContent;
    private Integer maxNum;
    private String bulletin;
    private Integer scrollTime;
    private String subtitle;
    private Integer rate;
    private Integer dataMode;
    private String startTime;
    private String endTime;
    private String isNum;
    private String isAudio;
    private String deviceCode;
    /** SCREEN / CLINIC */
    private String terminalType;
    /** 候诊模板路由 */
    private String templateRoute;

    /** 播报通道：VOICE | SCREEN | BOTH */
    private String voiceChannel;

    /** 本屏绑定的独立音响设备 ID 列表（多对多） */
    private List<String> voiceDeviceIds;

    /** 生命周期操作：FREEZE 冻结 | UNFREEZE 解除冻结；新增/编辑业务字段时不传 state，由服务端推算 */
    private String lifecycleAction;
}
