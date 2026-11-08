package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.dto.SignDeviceInput;
import com.gocent.pdjh.dto.SignDeviceOutput;
import com.gocent.pdjh.dto.SignRoomMappingInput;
import com.gocent.pdjh.service.SignDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/SignDevice")
@RequiredArgsConstructor
public class SignDeviceController {

    private final SignDeviceService signDeviceService;

    @GetMapping("/GetDeviceConfig")
    public ResultData<SignDeviceConfigOutput> getDeviceConfig(@RequestParam String deviceCode) {
        return ResultData.success(signDeviceService.getDeviceConfig(deviceCode), "获取成功");
    }

    @PostMapping("/list")
    public ResultData<List<SignDeviceOutput>> list(@RequestBody(required = false) Map<String, String> body) {
        String name = body != null ? body.get("name") : null;
        return ResultData.success(signDeviceService.listAll(name), "获取成功");
    }

    @PostMapping("/save")
    public ResultData<SignDeviceOutput> save(@RequestBody SignDeviceInput input) {
        return ResultData.success(signDeviceService.save(input), "保存成功");
    }

    @PostMapping("/remove")
    public ResultData<Boolean> remove(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        if (id == null || id.isBlank()) {
            return ResultData.fail(400, "id 不能为空");
        }
        return ResultData.success(signDeviceService.remove(id), "删除成功");
    }

    @PostMapping("/RelevancySignDeviceAndRooms")
    public ResultData<Boolean> relevancySignDeviceAndRooms(@RequestBody SignRoomMappingInput input) {
        if (input.getSignDeviceId() == null) {
            return ResultData.fail(400, "signDeviceId 不能为空");
        }
        boolean ok = signDeviceService.relevancySignDeviceAndRooms(input.getSignDeviceId(), input.getOfficeRoomIds());
        return ok ? ResultData.success(true, "绑定成功") : ResultData.fail("绑定失败");
    }
}
