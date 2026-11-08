package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.terminal.TerminalRegisterInput;
import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;
import com.gocent.pdjh.service.TerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 安卓 WebView 终端统一登记与解析（doc_screen 与 sign_device 平行）
 * 消费者：候诊区大屏、诊室门边屏、自助签到终端 BindingGuide
 */
@RestController
@RequestMapping("/Terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    /** 路径 A：终端自助创建待配置记录（幂等） */
    @PostMapping("/Register")
    public ResultData<TerminalRegisterOutput> register(@RequestBody TerminalRegisterInput input) {
        return terminalService.register(input);
    }

    /** 启动/轮询：解析终端配置就绪状态（404/428/403/200） */
    @GetMapping("/Resolve")
    public ResultData<TerminalResolveOutput> resolve(
            @RequestParam String deviceCode,
            @RequestParam String terminalType) {
        return terminalService.resolve(deviceCode, terminalType);
    }
}
