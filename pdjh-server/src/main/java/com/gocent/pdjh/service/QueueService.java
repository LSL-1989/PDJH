package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.EnqueueRequest;
import com.gocent.pdjh.entity.CallList;

public interface QueueService {
    /**
     * 将患者推入住院/门诊/药房的呼叫队列汇总表中
     * @param request 入队参数
     * @return 成功入队后的实体
     */
    CallList enqueue(EnqueueRequest request);

    /**
     * 手动签到 (供自助机或H5调用)
     * @param patientCode 患者编号
     * @return 签到成功返回 true
     */
    boolean signIn(String patientCode);
    
    /**
     * 获取待签到信息
     */
    CallList getPendingSignIn(String patientCode);
}
