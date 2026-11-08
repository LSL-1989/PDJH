package com.gocent.pdjh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RULE_* JSON 边界校验（说明书 §14.8 / R-8）。
 * 消费者：SysConfigService 保存规则键时；非 RULE_* 键不调用。
 */
public final class RuleConfigValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> PHARMACY_SPLIT = Set.of(
            "SHORTEST_QUEUE", "ROUND_ROBIN", "RANDOM", "FIXED_TYPE");
    private static final Set<String> EXAM_ALGO = Set.of(
            "FASTING_FIRST", "LEAST_WAIT", "SHORTEST_PATH", "TSP_GREEDY", "NONE");
    private static final String[] SMS_PLACEHOLDERS = {"{姓名}", "{科室}", "{号码}"};

    private RuleConfigValidator() {
    }

    /**
     * @param configKey 配置键
     * @param configValue JSON 字符串
     * @return 错误文案；空列表表示通过
     */
    public static List<String> validate(String configKey, String configValue) {
        List<String> errors = new ArrayList<>();
        if (configKey == null || !configKey.startsWith("RULE_")) {
            return errors;
        }
        if (configValue == null || configValue.isBlank()) {
            errors.add("规则内容不能为空");
            return errors;
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(configValue);
        } catch (Exception e) {
            errors.add("规则 JSON 无法解析");
            return errors;
        }
        if (!root.isObject()) {
            errors.add("规则必须是 JSON 对象");
            return errors;
        }
        switch (configKey) {
            case "RULE_OUTPATIENT" -> validateOutpatient(root, errors);
            case "RULE_PHARMACY" -> validatePharmacy(root, errors);
            case "RULE_EXAM" -> validateExam(root, errors);
            case "RULE_NOTIFICATION" -> validateNotification(root, errors);
            case "RULE_MEDICAL_TECH" -> validateMedtech(root, errors);
            default -> {
                // RULE_LAB 等其它规则键：只要是合法 JSON 对象即通过
            }
        }
        return errors;
    }

    private static void validateOutpatient(JsonNode n, List<String> errors) {
        range(n, "callTimeoutMin", 1, 60, errors);
        range(n, "maxSkipCount", 1, 10, errors);
        range(n, "preCallCount", 1, 10, errors);
        range(n, "smsAdvanceCount", 1, 10, errors);
        if (n.path("autoSplitEnabled").asBoolean(false)) {
            range(n, "splitThreshold", 5, 50, errors);
        }
    }

    private static void validatePharmacy(JsonNode n, List<String> errors) {
        range(n, "timeoutMinutes", 5, 120, errors);
        range(n, "alertMinutes", 5, 60, errors);
        range(n, "maxPendingPerWindow", 5, 50, errors);
        range(n, "firstReminderMin", 5, 30, errors);
        range(n, "repeatReminderMin", 5, 30, errors);
        String algo = text(n, "splitAlgorithm");
        if (algo != null && !PHARMACY_SPLIT.contains(algo)) {
            errors.add("splitAlgorithm 取值无效");
        }
    }

    private static void validateExam(JsonNode n, List<String> errors) {
        String algo = text(n, "triageAlgorithm");
        if (algo != null && !EXAM_ALGO.contains(algo)) {
            errors.add("triageAlgorithm 取值无效");
        }
        range(n, "itemTimeoutMin", 1, 240, errors);
        if (n.path("timeScheduleEnabled").asBoolean(false) && n.has("timeSchedules")
                && !n.get("timeSchedules").isArray()) {
            errors.add("timeSchedules 必须是数组");
        }
    }

    private static void validateNotification(JsonNode n, List<String> errors) {
        range(n, "callRepeatCount", 1, 5, errors);
        range(n, "callIntervalSec", 3, 30, errors);
        requirePlaceholders(n, "smsTemplateCall", errors);
        requirePlaceholders(n, "smsTemplateSkip", errors);
        requirePlaceholders(n, "smsTemplateAdvance", errors);
    }

    private static void validateMedtech(JsonNode n, List<String> errors) {
        if (n.has("lab")) {
            range(n.get("lab"), "callTimeoutMin", 1, 60, errors);
        }
    }

    private static void requirePlaceholders(JsonNode n, String field, List<String> errors) {
        if (!n.has(field) || n.get(field).isNull()) {
            return;
        }
        String t = n.get(field).asText("");
        if (t.isBlank()) {
            return;
        }
        for (String p : SMS_PLACEHOLDERS) {
            if (!t.contains(p)) {
                errors.add(field + " 缺少占位符 " + p);
            }
        }
    }

    private static void range(JsonNode n, String field, int min, int max, List<String> errors) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return;
        }
        JsonNode v = n.get(field);
        if (!v.isNumber()) {
            errors.add(field + " 必须是数字");
            return;
        }
        int i = v.asInt();
        if (i < min || i > max) {
            errors.add(field + " 须在 " + min + "–" + max + " 之间");
        }
    }

    private static String text(JsonNode n, String field) {
        if (!n.has(field) || n.get(field).isNull()) {
            return null;
        }
        String t = n.get(field).asText("");
        return t.isBlank() ? null : t.trim();
    }
}
