package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.service.TriageAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TriageAdminServiceImpl implements TriageAdminService {

    private final CallListRepository callListRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revive(Integer callListId) {
        if (callListId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null || !"PASSED".equals(callList.getTriageState())) return false;
        
        callList.setTriageState("WAITING");
        callList.setXh(99990); // 默认打回等待大群，顺延安置于队尾进行再次候补
        callListRepository.save(callList);
        log.info("【导医仲裁】特权判定：因过号被打入废弃冷宫的患者排队单 {} 被护士长跨权人工复活，列编回 WAITING 队尾继续排队受领。", callListId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceSwap(Integer callListId, String newDeptId, String newDoctorId) {
        if (callListId == null || newDeptId == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;

        callList.setDeptID(newDeptId);
        if (newDoctorId != null) {
            callList.setDoctorID(newDoctorId);
        }
        callListRepository.save(callList);
        log.info("【导医仲裁】特权判定：患者排队单 {} 因换源或退转，被管理员实施降维夺取并无缝平移流转至新科室阵列: {} / {}", callListId, newDeptId, newDoctorId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean manualCutIn(Integer callListId, Integer targetXh) {
        if (callListId == null || targetXh == null) return false;
        CallList callList = callListRepository.findById(callListId).orElse(null);
        if (callList == null) return false;

        callList.setTriageState("WAITING");
        // 特供硬插队如 xh = 0 或 xh = 特定排位
        callList.setXh(targetXh);
        callListRepository.save(callList);
        log.info("【导医仲裁】防核通道解锁！患者排队单 {} 被导医台护士长强行越规植入指定排序权重插队空降坐标: {}", callListId, targetXh);
        return true;
    }
}
