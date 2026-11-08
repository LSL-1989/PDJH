package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.clinic.ClinicDisplayVO;
import com.gocent.pdjh.exception.TerminalBindingException;
import com.gocent.pdjh.service.ClinicScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ClinicScreen")
@RequiredArgsConstructor
public class ClinicScreenController {

    private final ClinicScreenService clinicScreenService;

    /**
     * 门边屏展示数据。preview=true 时只读：不刷新 lastSeenAt，供管理后台预览，不影响现场在线与叫号。
     */
    @GetMapping("/GetDisplay")
    public ResultData<ClinicDisplayVO> getDisplay(
            @RequestParam String deviceCode,
            @RequestParam(required = false, defaultValue = "false") boolean preview) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return ResultData.fail(500, "设备识别码(deviceCode)不能为空");
        }
        try {
            ClinicDisplayVO vo = clinicScreenService.buildDisplay(deviceCode.trim(), preview);
            return ResultData.success(vo, "获取诊室屏展示数据成功");
        } catch (TerminalBindingException e) {
            return ResultData.fail(e.getCode(), e.getMessage());
        }
    }
}
