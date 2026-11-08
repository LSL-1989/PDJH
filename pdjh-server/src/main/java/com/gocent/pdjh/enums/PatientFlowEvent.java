package com.gocent.pdjh.enums;

/**
 * 患者就诊/取药全流程事件类型（统一留痕）
 * 消费者：PatientFlowLog、后台患者流程日志页
 */
public enum PatientFlowEvent {
    SIGN_IN("门诊签到"),
    CALL("医生叫号"),
    COMPLETE("就诊完成"),
    REVISIT("复诊"),
    SKIP("过号"),
    PHARMACY_SIGN_IN("取药签到"),
    PHARMACY_READY("配药完成"),
    PHARMACY_CALL("取药叫号"),
    PHARMACY_COMPLETE("取药完成"),
    PHARMACY_SKIP("取药过号"),
    LAB_SIGN_IN("检验签到"),
    LAB_CALL("检验叫号"),
    LAB_COMPLETE("采血完成"),
    LAB_SKIP("检验过号"),
    NURSE_MANUAL_SIGN_IN("护士代签到"),
    NURSE_REVIVE("过号复活"),
    NURSE_CUT_IN("手动插队"),
    NURSE_SWAP("转诊"),
    NURSE_FREEZE("冻结"),
    NURSE_UNFREEZE("解冻"),
    NURSE_REFUND("HIS退号作废"),
    NURSE_BIND_ROOM("补绑诊室"),
    OTHER("其他操作");

    private final String label;

    PatientFlowEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }
}
