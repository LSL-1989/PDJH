package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.PharmacyCheckInResult;

/**
 * 检验采血签到：扫码入队并分窗，直接进入 WAITING 可叫号（无药房配药环节）。
 * 与 pharmacyCheckIn 差异：药房先 PREPARING；与门诊 getSignInInfo 差异：门诊须先有 UNARRIVED 单。
 * HIS 已推 UNARRIVED 到本机绑定窗口时，核销该单而不新建。
 * 消费者：HJ自助签到终端 LAB 场景
 */
public interface LabCheckInService {

    /**
     * 患者扫码签到入队。
     * @param patientCode 就诊卡号/条码
     * @param deviceCode 签到机设备编号（须绑定检验诊室）
     * @return 入队结果（含窗口）；不可入队时抛 IllegalArgumentException
     */
    PharmacyCheckInResult checkIn(String patientCode, String deviceCode);
}
