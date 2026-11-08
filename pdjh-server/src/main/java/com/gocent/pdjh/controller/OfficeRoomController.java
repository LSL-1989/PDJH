package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.OfficeRoomInput;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.service.OfficeRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/officeRoom")
@RequiredArgsConstructor
public class OfficeRoomController {

    private final OfficeRoomService officeRoomService;

    @PostMapping("/list")
    public ResultData<List<OfficeRoom>> getAllRooms() {
        return ResultData.success(officeRoomService.getAllRooms(), "获取成功");
    }

    @PostMapping("/listByDept")
    public ResultData<List<OfficeRoom>> getRoomsByDeptId(@RequestBody Map<String, String> payload) {
        String deptId = payload.get("deptId");
        return ResultData.success(officeRoomService.getRoomsByDeptId(deptId), "获取成功");
    }

    @PostMapping("/save")
    public ResultData<Boolean> saveRoom(@RequestBody OfficeRoomInput input) {
        try {
            boolean ok = officeRoomService.saveRoom(input);
            return ok ? ResultData.success(true, "保存成功") : ResultData.fail(500, "保存失败");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ResultData<Boolean> removeRoom(@RequestBody Map<String, String> payload) {
        try {
            String id = payload.get("id");
            boolean ok = officeRoomService.removeRoom(id);
            return ok ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }
}
