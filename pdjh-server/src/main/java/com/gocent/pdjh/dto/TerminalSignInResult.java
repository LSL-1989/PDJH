package com.gocent.pdjh.dto;

import lombok.Data;

/**
 * 自助签到终端成功提示。
 * 消费者：Triage/CheckIn；字段与药房/检验 PharmacyCheckInResult 的队列字段对齐。
 */
@Data
public class TerminalSignInResult {
    private String showNo;
    private String patientName;
    private String patientCode;
    private String officeRoomName;
    /** 签到时刻 HH:mm:ss；重复签到为原签到时间 */
    private String checkInTime;
    /** 当前仍在等候的人数，含本人 */
    private Integer queueCount;
    /** 按签到时间先后的 1 起始位置；不在等候队列时为 0 */
    private Integer queuePosition;
    private boolean alreadyCheckedIn;
    private String message;
}
