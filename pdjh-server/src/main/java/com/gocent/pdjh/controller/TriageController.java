package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.TerminalSignInResult;
import com.gocent.pdjh.service.TriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/Triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @PostMapping("/CheckIn")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> request) {
        Integer callListId = request.get("callListId") instanceof Number
                ? ((Number) request.get("callListId")).intValue() : null;
        String deviceCode = request.get("deviceCode") != null
                ? request.get("deviceCode").toString()
                : (request.get("deviceId") != null ? request.get("deviceId").toString() : null);
        if (callListId == null) {
            return ResponseEntity.badRequest().body("callListId 不能为空");
        }
        if (deviceCode == null || deviceCode.isBlank()) {
            return ResponseEntity.badRequest().body("deviceCode 不能为空");
        }
        TerminalSignInResult prompt = triageService.checkInForTerminal(callListId, deviceCode);
        if (prompt == null) {
            return ResponseEntity.badRequest().body("患者状态签到条件非UNARRIVED错乱或未捕获此单！");
        }
        return ResponseEntity.ok(ResultData.success(prompt, prompt.getMessage()));
    }

    @PostMapping("/MarkNoShow")
    public ResponseEntity<?> markNoShow(@RequestBody Map<String, Integer> request) {
        boolean success = triageService.markNoShow(request.get("callListId"));
        return success ? ResponseEntity.ok("计入黑名单，满载 3 失联将抛弃！") : ResponseEntity.badRequest().body("失联处理失败或号已作古");
    }

    @PostMapping("/SuspendForTest")
    public ResponseEntity<?> suspendForTest(@RequestBody Map<String, Integer> request) {
        boolean success = triageService.suspendForTest(request.get("callListId"));
        return success ? ResponseEntity.ok("离去检车暂存至虚无池成功") : ResponseEntity.badRequest().body("操作异常或实体走失");
    }

    @PostMapping("/ReturnFromTest")
    public ResponseEntity<?> returnFromTest(@RequestBody Map<String, Integer> request) {
        boolean success = triageService.returnFromTest(request.get("callListId"));
        return success ? ResponseEntity.ok("携带化验单据载誉而归！赋予零号强权插队中..") : ResponseEntity.badRequest().body("操作不可靠，请出具化验确认单号");
    }
}
