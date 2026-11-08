package com.gocent.pdjh.enums;

/** 护士站干预原因码，与 HJ护士工作站 reasonCodes.js 对齐 */
public enum NurseActionReason {
    DEVICE_FAIL,
    PATIENT_ASSIST,
    SPECIAL_GROUP,
    APPT_OVERRIDE,
    NO_SECOND_SCAN,
    ROOM_FIX,
    HIS_REFUNDED,
    CONSULT_TRANSFER,
    OTHER
}
