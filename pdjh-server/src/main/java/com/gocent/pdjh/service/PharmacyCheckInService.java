package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.PharmacyCheckInResult;
import com.gocent.pdjh.entity.CallList;

/**
 * 药房辅助签到：扫码才入队、签到后分窗、通知 HIS 配药；配药完成后方可叫号
 * 消费者：自助签到机 PHARMACY 场景、药房工作站
 */
public interface PharmacyCheckInService {

    /**
     * 患者扫码签到入队。
     * @param patientCode 就诊卡号/处方条码
     * @param deviceCode 签到机设备编号（须绑定药房诊室）
     * @return 入队结果（含窗口）；不可入队时抛 IllegalArgumentException
     */
    PharmacyCheckInResult checkIn(String patientCode, String deviceCode);

    /**
     * 配药完成：PREPARING → WAITING，进入可叫号池
     * @param callListId 排队单 ID
     * @return 更新后的单据；不存在或状态不符时抛异常
     */
    CallList markReady(Integer callListId);

    /**
     * 药房窗口当日队列（配药中 + 请取药），供药房工作站列表
     */
    java.util.List<CallList> listDeskQueue(String officeRoomId);
}
