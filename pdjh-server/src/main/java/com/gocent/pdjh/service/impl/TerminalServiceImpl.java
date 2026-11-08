package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.terminal.TerminalRegisterInput;
import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.service.ScreenTerminalService;
import com.gocent.pdjh.service.SignTerminalService;
import com.gocent.pdjh.service.TerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TerminalServiceImpl implements TerminalService {

    private final ScreenTerminalService screenTerminalService;
    private final SignTerminalService signTerminalService;
    private final DocScreenRepository docScreenRepository;
    private final SignDeviceRepository signDeviceRepository;

    @Override
    public ResultData<TerminalRegisterOutput> register(TerminalRegisterInput input) {
        if (input.getDeviceCode() == null || input.getDeviceCode().isBlank()) {
            return ResultData.fail(400, "设备识别码(deviceCode)不能为空");
        }
        String terminalType = input.getTerminalType() != null ? input.getTerminalType().trim().toUpperCase() : "";
        if (!TerminalConstants.isValidTerminalType(terminalType)) {
            return ResultData.fail(400, "terminalType 无效，须为 SCREEN / CLINIC / CHECKIN");
        }

        String code = input.getDeviceCode().trim();
        String conflict = findCrossTableConflict(code, terminalType);
        if (conflict != null) {
            return ResultData.fail(409, conflict);
        }

        TerminalRegisterOutput output;
        if (TerminalConstants.TYPE_CHECKIN.equals(terminalType)) {
            output = signTerminalService.register(code);
        } else {
            output = screenTerminalService.register(code, terminalType);
        }
        return ResultData.success(output, output.isCreated() ? "终端登记成功，等待管理员完成配置" : "终端已登记");
    }

    @Override
    public ResultData<TerminalResolveOutput> resolve(String deviceCode, String terminalType) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return ResultData.fail(400, "设备识别码(deviceCode)不能为空");
        }
        String type = terminalType != null ? terminalType.trim().toUpperCase() : "";
        if (!TerminalConstants.isValidTerminalType(type)) {
            return ResultData.fail(400, "terminalType 无效，须为 SCREEN / CLINIC / CHECKIN");
        }

        TerminalResolveOutput output;
        if (TerminalConstants.TYPE_CHECKIN.equals(type)) {
            output = signTerminalService.resolve(deviceCode.trim());
        } else {
            output = screenTerminalService.resolve(deviceCode.trim(), type);
        }

        return mapResolveResult(output);
    }

    private ResultData<TerminalResolveOutput> mapResolveResult(TerminalResolveOutput output) {
        String status = output.getStatus();
        if (TerminalConstants.STATUS_NOT_REGISTERED.equals(status)) {
            return ResultData.fail(404, "设备未注册，请完成终端绑定或联系管理员");
        }
        if (TerminalConstants.STATUS_FROZEN.equals(status)) {
            return ResultData.fail(403, "当前设备已被停用或冻结，请联系管理员");
        }
        if (TerminalConstants.STATUS_PENDING.equals(status)) {
            return new ResultData<>(false, 428, "设备已登记，等待管理员完成配置（绑定诊室等）", output);
        }
        return ResultData.success(output, "终端配置就绪");
    }

    /** 登记前检查 deviceCode 是否已被另一张表占用 */
    private String findCrossTableConflict(String deviceCode, String terminalType) {
        if (TerminalConstants.TYPE_CHECKIN.equals(terminalType)) {
            if (docScreenRepository.findByDeviceCode(deviceCode).isPresent()) {
                return "设备编号已被大屏终端占用：" + deviceCode;
            }
        } else {
            if (signDeviceRepository.findByDeviceCode(deviceCode).isPresent()) {
                return "设备编号已被签到终端占用：" + deviceCode;
            }
        }
        return null;
    }
}
