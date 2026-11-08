package com.gocent.pdjh.dto.terminal;

import lombok.Data;

@Data
public class TerminalRegisterInput {
    private String deviceCode;
    /** SCREEN | CLINIC | CHECKIN */
    private String terminalType;
}
