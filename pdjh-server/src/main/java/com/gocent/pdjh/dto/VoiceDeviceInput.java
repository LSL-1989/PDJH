package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class VoiceDeviceInput {
    private String id;
    private String deviceCode;
    private String name;
    private String location;
    /** 绑定候诊大屏 ID */
    private String screenId;
    /** WS / HTTP / BOTH */
    private String playMode;
    private String endpointUrl;
    private Integer state;
}
