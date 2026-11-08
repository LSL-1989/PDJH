package com.gocent.pdjh.constants;

/** 分诊状态约定：与 call_list.triage_state 及护士站告警逻辑一致 */
public final class TriageStateConstants {

    private TriageStateConstants() {}

    public static final String WAITING = "WAITING";
    public static final String REVISIT = "REVISIT";
    public static final String UNARRIVED = "UNARRIVED";
    public static final String PASSED = "PASSED";
    public static final String TESTING = "TESTING";
    /** HIS 收费窗口已退号，护士 MarkHisRefunded 同步队列 */
    public static final String CANCELLED = "CANCELLED";
    /** 药房：已签到、配药中（不可叫号，配药完成后方可进入 WAITING） */
    public static final String PREPARING = "PREPARING";
}
