package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;

/** doc_screen 终端登记与解析（SCREEN / CLINIC） */
public interface ScreenTerminalService {

    TerminalRegisterOutput register(String deviceCode, String terminalType);

    TerminalResolveOutput resolve(String deviceCode, String terminalType);
}
