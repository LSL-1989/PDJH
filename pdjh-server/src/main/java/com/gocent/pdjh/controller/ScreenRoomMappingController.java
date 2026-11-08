package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.OfficeRoomBrief;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.ScreenRoomMappingInput;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.service.ScreenRoomMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ScreenRoomMapping")
@RequiredArgsConstructor
public class ScreenRoomMappingController {

    private final ScreenRoomMappingService screenRoomMappingService;

    @PostMapping("/GetHasassociatedRooms")
    public ResultData<List<OfficeRoomBrief>> getHasassociatedRooms(@RequestBody ScreensInput input) {
        if (input == null || input.getId() == null) {
            return ResultData.fail(400, "屏幕ID不能为空");
        }
        return ResultData.success(screenRoomMappingService.getAssociatedRooms(input.getId()), "获取成功");
    }

    @PostMapping("/RelevancyScreenAndRooms")
    public ResultData<Boolean> relevancyScreenAndRooms(@RequestBody ScreenRoomMappingInput input) {
        if (input.getScreenId() == null) {
            return ResultData.fail(400, "screenId 不能为空");
        }
        boolean ok = screenRoomMappingService.relevancyScreenAndRooms(input.getScreenId(), input.getOfficeRoomIds());
        return ok ? ResultData.success(true, "绑定成功") : ResultData.fail("绑定失败");
    }
}
