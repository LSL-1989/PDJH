package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.TerminalSignInResult;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.service.TriageService;
import com.gocent.pdjh.service.TerminalBindingService;
import com.gocent.pdjh.service.support.SignInQueueSupport;
import com.gocent.pdjh.service.support.SignInQueueSupport.SignInQueueFacts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final CallListRepository callListRepository;
    private final TerminalBindingService terminalBindingService;
    private final SignInQueueSupport signInQueueSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkIn(Integer callListId) {
        return checkInInternal(callListId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkIn(Integer callListId, String deviceCode) {
        java.util.List<String> allowedRoomIds = terminalBindingService.resolveSignDeviceRoomIds(deviceCode);
        return checkInInternal(callListId, allowedRoomIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TerminalSignInResult checkInForTerminal(Integer callListId, String deviceCode) {
        CallList before = callListId == null ? null : callListRepository.findById(callListId).orElse(null);
        if (before == null) {
            return null;
        }
        boolean already = !"UNARRIVED".equalsIgnoreCase(before.getTriageState());
        if (!checkIn(callListId, deviceCode)) {
            return null;
        }
        CallList after = callListRepository.findById(callListId).orElse(before);
        SignInQueueFacts facts = signInQueueSupport.locate(after);
        signInQueueSupport.recordTerminal(after, PatientFlowEvent.SIGN_IN, deviceCode, "签到终端", already, facts);
        TerminalSignInResult result = new TerminalSignInResult();
        result.setShowNo(after.getShowNo());
        result.setPatientName(after.getPatientName());
        result.setPatientCode(after.getPatientCode());
        result.setOfficeRoomName(after.getOutPatientOffice());
        result.setCheckInTime(signInQueueSupport.formatClock(after.getCheckInTime()));
        result.setQueueCount(facts.queueCount());
        result.setQueuePosition(facts.queuePosition());
        result.setAlreadyCheckedIn(already);
        result.setMessage(already
                ? "您已签到，请按当前序号继续等候叫号"
                : "请在诊室外等候，留意大屏幕叫号");
        return result;
    }

    private boolean checkInInternal(Integer callListId, java.util.List<String> allowedRoomIds) {
        if (callListId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;

        if (allowedRoomIds != null) {
            terminalBindingService.assertRoomAllowed(callList.getOfficeRoomId(), allowedRoomIds);
        }
        
        if ("UNARRIVED".equals(callList.getTriageState())) {
            callList.setTriageState("WAITING");
            callList.setCheckInTime(new Date());
            callListRepository.save(callList);
            log.info("【分诊引擎】患者排队单 {} 已扫码完成二次签到，进入 WAITING 等候池开闸放号！", callListId);
            return true;
        }
        // 已在排队中：幂等成功，不重复入队
        if ("WAITING".equals(callList.getTriageState()) || "REVISIT".equals(callList.getTriageState())
                || "CALLED".equals(callList.getTriageState()) || "VISITING".equals(callList.getTriageState())) {
            log.info("【分诊引擎】患者排队单 {} 重复扫码，当前状态 {} 拦截放行不重复处理。", callListId, callList.getTriageState());
            return true;
        }
        // 已完成/过号等终态不再自动变轨为复诊签到（REVISIT）
        log.info("【分诊引擎】患者排队单 {} 状态 {} 不允许复诊签到。", callListId, callList.getTriageState());
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markNoShow(Integer callListId) {
        if (callListId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;

        int currentCount = callList.getCallCount() != null ? callList.getCallCount() : 0;
        currentCount++;
        callList.setCallCount(currentCount);

        if (currentCount >= 3) {
            callList.setTriageState("PASSED");
            callList.setXh(999999);
            log.warn("【分诊引擎】严重警告！患者排队单 {} 已缺席达 3 次重连不中，被全自动打入过号冷宫 PASSED 淘汰！", callListId);
        } else {
            // 将其发配到队列末端但仍在 WAITING 序列给以警诫
            callList.setXh(99990 + currentCount); 
            log.info("【分诊引擎】患者排队单 {} 叫错 {} 次未达，受罚顺延发配队尾。", callListId, currentCount);
        }
        
        callListRepository.save(callList);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean suspendForTest(Integer callListId) {
        if (callListId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;
        
        callList.setTriageState("TESTING");
        callListRepository.save(callList);
        log.info("【分诊引擎】患者排队单 {} 暂离赴医技进行化验，自动流转为 TESTING 挂起剥离。", callListId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnFromTest(Integer callListId) {
        if (callListId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;
        
        callList.setTriageState("REVISIT");
        callList.setXh(0); // 最高优先降临防插队乱序
        callListRepository.save(callList);
        log.info("【分诊引擎】化验归来！患者排队单 {} 激活 REVISIT，注入顶级优先级抢占回流唤醒池进行插队速诊！", callListId);
        return true;
    }
}
