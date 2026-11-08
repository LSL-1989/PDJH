package com.gocent.pdjh.integration.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.GwMappingRule;
import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集成映射求值：按 JSONPath 或结果列抽出字段，并套用 CONST / MAP 表达式。
 * 消费者：数据源配置试跑、HTTP/JDBC 执行器、Webhook 解析。
 * 不访问数据库。
 */
public final class MappingEval {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MappingEval() {
    }

    /** 表达式只允许空、CONST:固定值、MAP:原值=新值,原值=新值。 */
    public static void checkExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        String expr = expression.trim();
        if (isConst(expr)) {
            if (expr.substring(6).trim().isEmpty()) {
                throw new IllegalArgumentException("CONST: 后面需要固定值");
            }
            return;
        }
        if (expr.regionMatches(true, 0, "MAP:", 0, 4)) {
            String body = expr.substring(4).trim();
            if (body.isEmpty()) {
                throw new IllegalArgumentException("MAP: 后面需要 原值=新值");
            }
            for (String pair : body.split(",")) {
                if (!pair.contains("=")) {
                    throw new IllegalArgumentException("MAP 项要写成 原值=新值，多项用逗号分隔");
                }
            }
            return;
        }
        throw new IllegalArgumentException("表达式仅支持 CONST:值 或 MAP:原值=新值");
    }

    public static Object applyExpression(Object value, String expression) {
        if (expression == null || expression.isBlank()) {
            return value;
        }
        String expr = expression.trim();
        if (isConst(expr)) {
            return expr.substring(6).trim();
        }
        if (expr.regionMatches(true, 0, "MAP:", 0, 4)) {
            String key = value == null ? "" : value.toString().trim();
            for (String pair : expr.substring(4).split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && kv[0].trim().equals(key)) {
                    return kv[1].trim();
                }
            }
        }
        return value;
    }

    /** HTTP / Webhook 样本：按 JSONPath 抽成行，CONST 规则不读源字段。 */
    public static List<Map<String, Object>> fromJson(String json, List<GwMappingRule> rules) {
        List<GwMappingRule> extractors = new ArrayList<>();
        List<GwMappingRule> constants = new ArrayList<>();
        split(rules, extractors, constants);

        Map<String, List<Object>> columns = new HashMap<>();
        int maxRows = constants.isEmpty() ? 0 : 1;
        for (GwMappingRule rule : extractors) {
            Object extracted;
            try {
                extracted = JsonPath.read(json, rule.getSourceField());
            } catch (Exception e) {
                throw new IllegalArgumentException("字段提取失败 " + rule.getSourceField() + "：" + e.getMessage());
            }
            List<Object> list = new ArrayList<>();
            if (extracted instanceof List) {
                list.addAll((List<?>) extracted);
            } else {
                list.add(extracted);
            }
            columns.put(rule.getTargetField(), list);
            maxRows = Math.max(maxRows, list.size());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < maxRows; i++) {
            Map<String, Object> row = new HashMap<>();
            for (GwMappingRule rule : extractors) {
                List<Object> col = columns.get(rule.getTargetField());
                Object raw = (col != null && col.size() > i) ? col.get(i) : null;
                row.put(rule.getTargetField(), applyExpression(raw, rule.getRuleExpression()));
            }
            putConstants(row, constants);
            rows.add(row);
        }
        return rows;
    }

    /** JDBC 试跑样本：JSON 对象或对象数组，键是列名。 */
    public static List<Map<String, Object>> fromRows(String json, List<GwMappingRule> rules) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("样本不是合法 JSON");
        }
        List<JsonNode> nodes = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(nodes::add);
        } else if (root.isObject()) {
            nodes.add(root);
        } else {
            throw new IllegalArgumentException("JDBC 样本需要 JSON 对象或对象数组");
        }
        List<GwMappingRule> extractors = new ArrayList<>();
        List<GwMappingRule> constants = new ArrayList<>();
        split(rules, extractors, constants);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("JDBC 样本的每一行都要是对象");
            }
            Map<String, Object> row = new HashMap<>();
            for (GwMappingRule rule : extractors) {
                JsonNode cell = node.get(rule.getSourceField());
                Object raw = cell == null || cell.isNull() ? null : cell.isValueNode() ? cell.asText() : cell.toString();
                row.put(rule.getTargetField(), applyExpression(raw, rule.getRuleExpression()));
            }
            putConstants(row, constants);
            rows.add(row);
        }
        return rows;
    }

    /** 已从 ResultSet 取出的列值，套用表达式。 */
    public static Object mapCell(Object value, GwMappingRule rule) {
        if (rule != null && isConst(rule.getRuleExpression())) {
            return applyExpression(null, rule.getRuleExpression());
        }
        return applyExpression(value, rule == null ? null : rule.getRuleExpression());
    }

    private static void split(List<GwMappingRule> rules, List<GwMappingRule> extractors, List<GwMappingRule> constants) {
        if (rules == null) {
            return;
        }
        for (GwMappingRule rule : rules) {
            if (isConst(rule.getRuleExpression())) {
                constants.add(rule);
            } else {
                extractors.add(rule);
            }
        }
    }

    private static void putConstants(Map<String, Object> row, List<GwMappingRule> constants) {
        for (GwMappingRule rule : constants) {
            row.put(rule.getTargetField(), applyExpression(null, rule.getRuleExpression()));
        }
    }

    private static boolean isConst(String expression) {
        return expression != null && expression.trim().regionMatches(true, 0, "CONST:", 0, 6);
    }
}
