package com.gocent.pdjh.controller;

import com.gocent.pdjh.service.TriageAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/TriageAdmin")
@RequiredArgsConstructor
public class TriageAdminController {

    private final TriageAdminService triageAdminService;

    @PostMapping("/Revive")
    public ResponseEntity<?> revive(@RequestBody Map<String, Integer> request) {
        boolean success = triageAdminService.revive(request.get("callListId"));
        return success ? ResponseEntity.ok("人工仲裁打捞干预成功！名单已被复生入列。") : ResponseEntity.badRequest().body("强行复原失败，请核对患者确系出局抛弃单");
    }

    @PostMapping("/ForceSwap")
    public ResponseEntity<?> forceSwap(@RequestBody Map<String, String> request) {
        Integer callListId = Integer.valueOf(request.get("callListId"));
        boolean success = triageAdminService.forceSwap(callListId, request.get("newDeptId"), request.get("newDoctorId"));
        return success ? ResponseEntity.ok("患者归属科室变更大洗牌锁定，队列已平移且不触发重排挂失！") : ResponseEntity.badRequest().body("跨服漂移异常或该节点科室丢失标的");
    }

    @PostMapping("/ManualCutIn")
    public ResponseEntity<?> manualCutIn(@RequestBody Map<String, Integer> request) {
        boolean success = triageAdminService.manualCutIn(request.get("callListId"), request.get("targetXh"));
        return success ? ResponseEntity.ok("军烈保障或大特供硬插队指令下达，完成定点强征插位权！") : ResponseEntity.badRequest().body("降权覆盖异常或目标位已被实体封死");
    }
}
