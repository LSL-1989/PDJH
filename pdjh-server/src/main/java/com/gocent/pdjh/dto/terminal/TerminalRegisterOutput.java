package com.gocent.pdjh.dto.terminal;

import lombok.Data;

@Data
public class TerminalRegisterOutput {
    private String recordId;
    private String deviceCode;
    private String terminalType;
    /** doc_screen.state 或 sign_device.status */
    private Integer lifecycleState;
    /** 本次是否新创建 */
    private boolean created;
}
