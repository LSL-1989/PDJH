package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.SignInfo;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.SignInfoRepository;
import com.gocent.pdjh.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/SignInfo")
@RequiredArgsConstructor
public class SignInfoController {

    private final SignInfoRepository signInfoRepository;
    private final CallListRepository callListRepository;
    private final QueueService queueService;

    @GetMapping("/GetSignInfoByCode")
    public ResultData<Map<String, Object>> getSignInfoByCode(@RequestParam String code) {
        SignInfo info = signInfoRepository.findByCode(code).orElseGet(() -> defaultSignInfo(code));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("Gid", info.getGid());
        out.put("Code", info.getCode());
        out.put("Name", info.getName());
        out.put("Title", info.getTitle());
        out.put("DeptInfo", info.getDeptInfo());
        out.put("IsQueue", info.getIsQueue());
        out.put("SignView", info.getSignView());
        out.put("ShowText", info.getShowText());
        out.put("Status", info.getStatus());
        return ResultData.success(out, "获取成功");
    }

    @PostMapping("/GetCreditCardData")
    public ResultData<Map<String, Object>> getCreditCardData(@RequestBody Map<String, String> body) {
        String cardNo = body.get("CardNo");
        if (cardNo == null || cardNo.isBlank()) {
            return ResultData.fail(400, "卡号不能为空");
        }
        CallList call = findTodayCall(cardNo);
        Map<String, Object> out = new LinkedHashMap<>();
        if (call != null) {
            out.put("PatientID", call.getPatientCode());
            out.put("PatientCode", call.getPatientCode());
            out.put("PatientName", call.getPatientName());
            out.put("DeptID", call.getDeptID());
            out.put("ShowNo", call.getShowNo());
            out.put("State", call.getState());
            out.put("CheckType", call.getSourceType());
            out.put("RecordDate", call.getCallDate());
        } else {
            out.put("PatientID", cardNo);
            out.put("PatientCode", cardNo);
            out.put("PatientName", "待确认患者");
            out.put("State", 7);
        }
        return ResultData.success(out, "查询成功");
    }

    @PostMapping("/VerifyPatientSign")
    public ResultData<Map<String, Object>> verifyPatientSign(@RequestBody Map<String, String> body) {
        String cardNo = body.get("PatientID");
        if (cardNo == null) cardNo = body.get("CardNo");
        CallList call = findTodayCall(cardNo);
        Map<String, Object> out = new LinkedHashMap<>();
        boolean signed = call != null && call.getState() != null && call.getState() != 7;
        out.put("IsSign", signed);
        out.put("AlreadySigned", signed);
        out.put("CanSign", call != null);
        out.put("Message", call == null ? "未找到待签到记录" : "可以签到");
        return ResultData.success(out, "验证完成");
    }

    @PostMapping("/ConfirmPatientSign")
    public ResultData<Map<String, Object>> confirmPatientSign(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> patientInfo = body.get("PatientInfo") instanceof Map
                ? (Map<String, Object>) body.get("PatientInfo") : body;
        String cardNo = str(patientInfo, "PatientID", "PatientCode", "patientCode");
        CallList call = findTodayCall(cardNo);
        if (call == null) {
            EnqueueRequest req = new EnqueueRequest();
            req.setPatientCode(cardNo);
            req.setPatientName(str(patientInfo, "PatientName", "patientName", null));
            req.setDeptID(str(patientInfo, "DeptID", "deptID", null));
            req.setRequireSignIn(false);
            call = queueService.enqueue(req);
        } else if (call.getState() != null && call.getState() == 7) {
            call.setState(0);
            callListRepository.save(call);
        }
        final CallList signedCall = call;
        int waitAhead = 0;
        if (signedCall != null && signedCall.getDeptID() != null) {
            waitAhead = (int) callListRepository.findTodayCallListByDeptAndState(signedCall.getDeptID(), 0).stream()
                    .filter(c -> c.getXh() != null && signedCall.getXh() != null && c.getXh() < signedCall.getXh())
                    .count();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("Success", true);
        out.put("xh", signedCall != null ? signedCall.getXh() : 0);
        out.put("xhlb", signedCall != null && signedCall.getXhlb() != null ? signedCall.getXhlb() : "");
        out.put("TopOrder", waitAhead);
        out.put("ShowNo", signedCall != null ? signedCall.getShowNo() : "");
        return ResultData.success(out, "签到成功");
    }

    private CallList findTodayCall(String cardNo) {
        if (cardNo == null || cardNo.isBlank()) return null;
        return callListRepository.findAll().stream()
                .filter(c -> cardNo.equals(c.getPatientCode()) || cardNo.equals(c.getShowNo()))
                .max(Comparator.comparingInt(c -> c.getId() != null ? c.getId() : 0))
                .orElse(null);
    }

    private static String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            if (k != null && m.get(k) != null) return m.get(k).toString();
        }
        return null;
    }

    private SignInfo defaultSignInfo(String code) {
        SignInfo info = new SignInfo();
        info.setGid(UUID.randomUUID().toString());
        info.setCode(code);
        info.setName("排队叫号签到端");
        info.setTitle("排队叫号签到端");
        info.setIsQueue(1);
        info.setStatus(1);
        info.setShowText("请输入流水序号或刷就诊卡签到！");
        return info;
    }
}
