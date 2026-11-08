package com.gocent.pdjh.dto.terminal;

import lombok.Data;

import java.util.Map;

@Data
public class TerminalResolveOutput {
    /** NOT_REGISTERED | PENDING | FROZEN | READY */
    private String status;
    private String deviceCode;
    private String terminalType;
    private String recordId;
    private String screenId;
    private String signDeviceId;
    private String templateRoute;
    private Map<String, Object> contentConfig;
    private String title;
    private String boundRoomSummary;
}
