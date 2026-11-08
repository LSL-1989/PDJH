package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;

/** sign_device 终端登记与解析（CHECKIN，与 doc_screen 平行） */
public interface SignTerminalService {

    TerminalRegisterOutput register(String deviceCode);

    TerminalResolveOutput resolve(String deviceCode);
}
