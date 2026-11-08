package com.gocent.pdjh.dto;

import lombok.Data;

/**
 * 扫码入队签到结果（药房/检验共用字段形状，避免签到机两套解析）
 * 消费者：HJ自助签到终端 PHARMACY、LAB 场景
 */
@Data
public class PharmacyCheckInResult {
    private Integer callListId;
    private String patientCode;
    private String patientName;
    private String showNo;
    private String deptId;
    private String officeRoomId;
    private String officeRoomName;
    private String triageState;
    private boolean alreadyCheckedIn;
    private String message;
    /** 签到时刻 HH:mm:ss；重复签到为原签到时间 */
    private String checkInTime;
    /** 当前仍在等候的人数，含本人 */
    private Integer queueCount;
    /** 按签到时间先后的 1 起始位置；不在等候队列时为 0 */
    private Integer queuePosition;
}
