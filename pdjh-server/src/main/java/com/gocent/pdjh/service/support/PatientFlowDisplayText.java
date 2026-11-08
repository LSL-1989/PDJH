package com.gocent.pdjh.service.support;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 患者流程日志展示文案：来源中文名、备注由状态码改成结果句。
 * 消费者：PatientFlowLog 查询出参、医生叫号写留痕。
 */
public final class PatientFlowDisplayText {

    private static final Pattern STATE_REMARK = Pattern.compile("(?i)^state\\s*=\\s*(\\d+)$");
    private static final Pattern FAIL_REASON = Pattern.compile("(?i)^(fail|failed|error|失败)\\s*[:=]\\s*(.+)$");
    private static final Map<String, String> SOURCE_LABELS = Map.of(
            "TERMINAL", "签到终端",
            "DOCTOR", "医生站",
            "NURSE", "护士站",
            "PHARMACY", "药房",
            "LAB", "检验",
            "SYS", "系统"
    );
    private static final Map<String, String> REASON_LABELS = Map.of(
            "DEVICE_FAIL", "自助机故障",
            "PATIENT_ASSIST", "患者不会操作",
            "SPECIAL_GROUP", "特殊人群协助",
            "APPT_OVERRIDE", "预约时段例外",
            "NO_SECOND_SCAN", "跳过二次签到",
            "ROOM_FIX", "补绑诊室",
            "HIS_REFUNDED", "HIS已退号",
            "CONSULT_TRANSFER", "会诊转诊",
            "OTHER", "其他原因"
    );

    private PatientFlowDisplayText() {
    }

    /** 操作端代码转中文；未知代码原样返回 */
    public static String sourceLabel(String source) {
        if (source == null || source.isBlank()) {
            return "系统";
        }
        String key = source.trim().toUpperCase(Locale.ROOT);
        return SOURCE_LABELS.getOrDefault(key, source.trim());
    }

    /**
     * 把 state=1 这类备注换成结果句。已经是中文说明的原样返回。
     * eventType 用于区分同一状态码在门诊/药房/检验下的含义。
     */
    public static String humanizeRemark(String remark, String eventType) {
        if (remark == null || remark.isBlank()) {
            return null;
        }
        String text = remark.trim();
        Matcher state = STATE_REMARK.matcher(text);
        if (state.matches()) {
            return stateSentence(eventType, Integer.parseInt(state.group(1)));
        }
        Matcher fail = FAIL_REASON.matcher(text);
        if (fail.matches()) {
            return "失败，原因：" + fail.group(2).trim();
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if ("TIMEOUT".equals(upper) || "TIME_OUT".equals(upper)) {
            return "操作超时";
        }
        if ("FAIL".equals(upper) || "FAILED".equals(upper) || "ERROR".equals(upper)) {
            return "操作失败";
        }
        String reason = REASON_LABELS.get(upper);
        if (reason != null) {
            return reason;
        }
        return text;
    }

    /** 叫号状态变更成功后的备注，供写入 patient_flow_log */
    public static String remarkForState(String eventType, int state) {
        return stateSentence(eventType, state);
    }

    private static String stateSentence(String eventType, int state) {
        String event = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        String specific = switch (event) {
            case "CALL" -> state == 1 ? "叫号成功，患者开始就诊" : null;
            case "PHARMACY_CALL" -> state == 1 ? "叫号成功，请患者到窗口取药" : null;
            case "LAB_CALL" -> state == 1 ? "叫号成功，患者开始采血" : null;
            case "COMPLETE" -> state == 4 ? "就诊完成" : null;
            case "PHARMACY_COMPLETE" -> state == 4 ? "取药完成" : null;
            case "LAB_COMPLETE" -> state == 4 ? "采血完成" : null;
            case "SKIP" -> state == 6 ? "已过号" : null;
            case "PHARMACY_SKIP" -> state == 6 ? "取药已过号" : null;
            case "LAB_SKIP" -> state == 6 ? "检验已过号" : null;
            default -> null;
        };
        if (specific != null) {
            return specific;
        }
        return switch (state) {
            case 0 -> "成功，已回到等候";
            case 1 -> "叫号成功";
            case 4 -> "办理完成";
            case 6 -> "已过号";
            case 7 -> "尚未签到";
            default -> "操作已完成";
        };
    }
}
