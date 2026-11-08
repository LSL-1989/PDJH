package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.VoiceDeviceInput;
import com.gocent.pdjh.dto.VoiceDeviceOutput;
import com.gocent.pdjh.dto.VoiceScopeMappingInput;
import com.gocent.pdjh.entity.VoiceDevice;
import com.gocent.pdjh.service.VoiceDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/VoiceDevice")
@RequiredArgsConstructor
public class VoiceDeviceController {

    private final VoiceDeviceService voiceDeviceService;

    @PostMapping("/list")
    public ResultData<List<VoiceDeviceOutput>> list() {
        return ResultData.success(voiceDeviceService.listAll(), "获取成功");
    }

    @GetMapping("/GetDeviceConfig")
    public ResultData<VoiceDeviceOutput> getDeviceConfig(@RequestParam String deviceCode) {
        try {
            return ResultData.success(voiceDeviceService.getByDeviceCode(deviceCode), "获取成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    @PostMapping("/save")
    public ResultData<VoiceDevice> save(@RequestBody VoiceDeviceInput input) {
        try {
            return ResultData.success(voiceDeviceService.save(input), "保存成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ResultData<Boolean> remove(@RequestBody Map<String, String> body) {
        String id = body != null ? body.get("id") : null;
        if (id == null || id.isBlank()) {
            return ResultData.fail(400, "id 不能为空");
        }
        return ResultData.success(voiceDeviceService.remove(id), "删除成功");
    }

    /** 绑定多个诊室与诊区 */
    @PostMapping("/RelevancyScopes")
    public ResultData<Boolean> relevancyScopes(@RequestBody VoiceScopeMappingInput input) {
        try {
            return ResultData.success(voiceDeviceService.bindScopes(input), "绑定成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }
}
