package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.terminal.TerminalRegisterInput;
import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;

/** 安卓 WebView 终端统一登记与解析 */
public interface TerminalService {

    ResultData<TerminalRegisterOutput> register(TerminalRegisterInput input);

    ResultData<TerminalResolveOutput> resolve(String deviceCode, String terminalType);
}
