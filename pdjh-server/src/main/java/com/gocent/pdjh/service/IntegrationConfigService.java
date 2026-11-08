package com.gocent.pdjh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.GwConfigHistory;
import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.integration.engine.MappingEval;
import com.gocent.pdjh.repository.GwConfigHistoryRepository;
import com.gocent.pdjh.repository.GwConnectionRepository;
import com.gocent.pdjh.repository.GwEndpointRepository;
import com.gocent.pdjh.repository.GwMappingRuleRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据源接口配置：连接、端点、映射的保存、试跑与回滚。
 * 消费者：IntegrationConfigController、模板导入。
 * 依赖：gw_connection、gw_endpoint、gw_mapping_rule、gw_config_history。
 */
@Service
public class IntegrationConfigService {

    @Autowired
    private GwConnectionRepository connectionRepository;

    @Autowired
    private GwEndpointRepository endpointRepository;

    @Autowired
    private GwMappingRuleRepository mappingRuleRepository;

    @Autowired
    private GwConfigHistoryRepository historyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 列出连接。密码不返回，仅用 passwordSet 表示是否已保存。 */
    public List<GwConnection> getAllConnections() {
        List<GwConnection> all = connectionRepository.findAll();
        all.forEach(this::present);
        return all;
    }

    /**
     * 新增或更新连接。密码留空且已有记录时保留原密码。
     * type：HTTP / JDBC / WEBHOOK。
     */
    public GwConnection saveConnection(GwConnection conn) {
        boolean isNew = conn.getId() == null || conn.getId().isBlank();
        GwConnection existing = isNew ? null : connectionRepository.findById(conn.getId()).orElse(null);
        if (!isNew && existing == null) {
            throw new IllegalArgumentException("连接不存在");
        }
        if (!isNew && (conn.getPassword() == null || conn.getPassword().isBlank())) {
            conn.setPassword(existing.getPassword());
        }
        if (!isNew && conn.getCreateTime() == null) {
            conn.setCreateTime(existing.getCreateTime());
        }
        validateConnection(conn);
        if (isNew) {
            conn.setId(UUID.randomUUID().toString());
            conn.setCreateTime(new Date());
        }
        if (conn.getStatus() == null) {
            conn.setStatus(1);
        }
        conn.setUpdateTime(new Date());
        return connectionRepository.save(conn);
    }

    /** 删除连接。仍有端点时拒绝，避免留下孤儿映射。 */
    public void deleteConnection(String id) {
        List<GwEndpoint> endpoints = endpointRepository.findByConnectionId(id);
        if (endpoints != null && !endpoints.isEmpty()) {
            throw new IllegalArgumentException("请先删除该连接下的端点");
        }
        connectionRepository.deleteById(id);
    }

    public List<GwEndpoint> getEndpointsByConnection(String connectionId) {
        return endpointRepository.findByConnectionId(connectionId);
    }

    /** 新增或更新端点，并记快照。operator 为空时记 ADMIN。 */
    public GwEndpoint saveEndpoint(GwEndpoint endpoint, String operator) {
        boolean isNew = endpoint.getId() == null || endpoint.getId().isBlank();
        if (isNew) {
            endpoint.setId(UUID.randomUUID().toString());
            endpoint.setCreateTime(new Date());
        }
        if (endpoint.getStatus() == null) {
            endpoint.setStatus(1);
        }
        if (endpoint.getEnvMode() == null) {
            endpoint.setEnvMode(0);
        }
        validateEndpoint(endpoint);
        endpoint.setUpdateTime(new Date());
        GwEndpoint saved = endpointRepository.save(endpoint);
        takeEndpointSnapshot(saved.getId(), operator, isNew ? "CREATE" : "UPDATE");
        return saved;
    }

    public void deleteEndpoint(String id, String operator) {
        takeEndpointSnapshot(id, operator, "DELETE");
        List<GwMappingRule> rules = mappingRuleRepository.findByEndpointId(id);
        if (rules != null && !rules.isEmpty()) {
            mappingRuleRepository.deleteAll(rules);
        }
        endpointRepository.deleteById(id);
    }

    public List<GwMappingRule> getMappingRulesByEndpoint(String endpointId) {
        return mappingRuleRepository.findByEndpointId(endpointId);
    }

    public GwMappingRule saveMappingRule(GwMappingRule rule, String operator) {
        validateMapping(rule);
        if (rule.getId() == null || rule.getId().isBlank()) {
            rule.setId(UUID.randomUUID().toString());
            rule.setCreateTime(new Date());
        }
        GwMappingRule saved = mappingRuleRepository.save(rule);
        takeEndpointSnapshot(saved.getEndpointId(), operator, "UPDATE_MAPPING");
        return saved;
    }

    public void deleteMappingRule(String id, String operator) {
        GwMappingRule rule = mappingRuleRepository.findById(id).orElse(null);
        if (rule != null) {
            String endpointId = rule.getEndpointId();
            mappingRuleRepository.deleteById(id);
            takeEndpointSnapshot(endpointId, operator, "DELETE_MAPPING");
        }
    }

    /**
     * 用已保存的映射试跑样本，不访问院方。
     * JDBC 连接认列名 JSON；HTTP / WEBHOOK 认 JSONPath。
     */
    public List<Map<String, Object>> dryRun(String endpointId, String mockData) {
        if (endpointId == null || endpointId.isBlank()) {
            throw new IllegalArgumentException("endpointId 不能为空");
        }
        if (mockData == null || mockData.isBlank()) {
            throw new IllegalArgumentException("请粘贴样本数据");
        }
        GwEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在"));
        List<GwMappingRule> rules = mappingRuleRepository.findByEndpointId(endpointId);
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("请先保存至少一条映射");
        }
        GwConnection conn = connectionRepository.findById(endpoint.getConnectionId()).orElse(null);
        if (conn != null && "JDBC".equalsIgnoreCase(conn.getType())) {
            return MappingEval.fromRows(mockData, rules);
        }
        return MappingEval.fromJson(mockData, rules);
    }

    /**
     * 被动回调白名单。没有对应的启用 WEBHOOK 端点，或白名单为空时不拦截。
     * @return null 表示放行，否则是拒绝原因
     */
    public String webhookDenyReason(String endpointCode, String remoteIp) {
        if (endpointCode == null || endpointCode.isBlank()) {
            return null;
        }
        GwEndpoint hit = null;
        for (GwEndpoint endpoint : endpointRepository.findAll()) {
            if (endpointCode.equalsIgnoreCase(endpoint.getEndpointCode())
                    && "WEBHOOK".equalsIgnoreCase(endpoint.getMethod())
                    && (endpoint.getStatus() == null || endpoint.getStatus() == 1)) {
                hit = endpoint;
                break;
            }
        }
        if (hit == null) {
            return null;
        }
        GwConnection conn = connectionRepository.findById(hit.getConnectionId()).orElse(null);
        if (conn == null || (conn.getStatus() != null && conn.getStatus() == 0)) {
            return "Webhook 连接已停用";
        }
        if (!"WEBHOOK".equalsIgnoreCase(conn.getType())) {
            return null;
        }
        String allow = conn.getAllowIps();
        if (allow == null || allow.isBlank() || allow.contains("*")) {
            return null;
        }
        String ip = remoteIp == null ? "" : remoteIp.trim();
        for (String token : allow.split("[,;\\s]+")) {
            if (ip.equals(token.trim())) {
                return null;
            }
        }
        return "来源 IP 不在白名单：" + ip;
    }

    public GwConnection getConnection(String id) {
        return connectionRepository.findById(id).orElse(null);
    }

    /** 响应前清掉密码。调用时保存事务已结束，不会把空密码写回库。 */
    public void present(GwConnection conn) {
        if (conn == null) {
            return;
        }
        boolean set = conn.getPassword() != null && !conn.getPassword().isBlank();
        conn.setPasswordSet(set);
        conn.setPassword(null);
    }

    private void takeEndpointSnapshot(String endpointId, String operator, String operationType) {
        try {
            GwEndpoint endpoint = endpointRepository.findById(endpointId).orElse(null);
            if (endpoint == null && !"DELETE".equals(operationType)) {
                return;
            }
            List<GwMappingRule> rules = mappingRuleRepository.findByEndpointId(endpointId);
            Map<String, Object> snapshotData = new HashMap<>();
            snapshotData.put("endpoint", endpoint);
            snapshotData.put("rules", rules);
            GwConfigHistory history = new GwConfigHistory();
            history.setEntityType("ENDPOINT_FULL");
            history.setEntityId(endpointId);
            history.setSnapshotJson(objectMapper.writeValueAsString(snapshotData));
            history.setOperationType(operationType);
            history.setOperator(operator == null || operator.isBlank() ? "ADMIN" : operator.trim());
            history.setCreateTime(new Date());
            historyRepository.save(history);
        } catch (Exception e) {
            System.err.println("Failed to take config snapshot: " + e.getMessage());
        }
    }

    public List<GwConfigHistory> getEndpointHistory(String endpointId) {
        return historyRepository.findByEntityTypeAndEntityIdOrderByCreateTimeDesc("ENDPOINT_FULL", endpointId);
    }

    public void rollbackEndpoint(Long historyId, String operator) throws Exception {
        GwConfigHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("快照不存在"));
        Map<String, Object> snapshotData = objectMapper.readValue(history.getSnapshotJson(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        GwEndpoint oldEndpoint = objectMapper.convertValue(snapshotData.get("endpoint"), GwEndpoint.class);
        if (oldEndpoint == null) {
            throw new IllegalArgumentException("该快照没有端点内容，不能回滚");
        }
        List<GwMappingRule> oldRules = objectMapper.convertValue(snapshotData.get("rules"),
                new com.fasterxml.jackson.core.type.TypeReference<List<GwMappingRule>>() {});
        oldEndpoint.setUpdateTime(new Date());
        endpointRepository.save(oldEndpoint);
        List<GwMappingRule> currentRules = mappingRuleRepository.findByEndpointId(oldEndpoint.getId());
        mappingRuleRepository.deleteAll(currentRules);
        if (oldRules != null && !oldRules.isEmpty()) {
            mappingRuleRepository.saveAll(oldRules);
        }
        takeEndpointSnapshot(oldEndpoint.getId(), operator, "ROLLBACK");
    }

    /** @return 成功说明；失败抛 IllegalArgumentException */
    public String pingConnection(String id) {
        GwConnection conn = connectionRepository.findById(id).orElse(null);
        if (conn == null) {
            throw new IllegalArgumentException("连接不存在");
        }
        if ("WEBHOOK".equalsIgnoreCase(conn.getType())) {
            return "被动接收不用探测，白名单在回调时生效";
        }
        if ("HTTP".equalsIgnoreCase(conn.getType())) {
            try {
                URL url = new URL(conn.getBaseUrl());
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode >= 200 && responseCode < 400) {
                    return "HTTP 连接可用，状态 " + responseCode;
                }
                throw new IllegalArgumentException("HTTP 返回 " + responseCode);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("HTTP 连接失败");
            }
        }
        if ("JDBC".equalsIgnoreCase(conn.getType())) {
            try {
                Class.forName(conn.getDriverClass());
                try (Connection dbConnection = DriverManager.getConnection(conn.getBaseUrl(), conn.getUsername(), conn.getPassword())) {
                    if (dbConnection.isValid(5)) {
                        return "JDBC 连接可用";
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("JDBC 连接失败");
            }
        }
        throw new IllegalArgumentException("无法探测该连接");
    }

    private void validateConnection(GwConnection conn) {
        if (conn.getName() == null || conn.getName().isBlank()) {
            throw new IllegalArgumentException("请填写连接名称");
        }
        String type = conn.getType() == null ? "" : conn.getType().trim().toUpperCase();
        if (!type.equals("HTTP") && !type.equals("JDBC") && !type.equals("WEBHOOK")) {
            throw new IllegalArgumentException("连接类型只支持 HTTP、JDBC、WEBHOOK");
        }
        conn.setType(type);
        if (conn.getStatus() != null && conn.getStatus() != 0 && conn.getStatus() != 1) {
            throw new IllegalArgumentException("状态只能是启用或停用");
        }
        if ("HTTP".equals(type)) {
            if (conn.getBaseUrl() == null || conn.getBaseUrl().isBlank()) {
                throw new IllegalArgumentException("请填写 HTTP 地址");
            }
            String url = conn.getBaseUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new IllegalArgumentException("HTTP 地址要以 http:// 或 https:// 开头");
            }
            conn.setBaseUrl(url);
        } else if ("JDBC".equals(type)) {
            if (conn.getBaseUrl() == null || !conn.getBaseUrl().trim().toLowerCase().startsWith("jdbc:")) {
                throw new IllegalArgumentException("JDBC 地址要以 jdbc: 开头");
            }
            if (conn.getDriverClass() == null || conn.getDriverClass().isBlank()) {
                throw new IllegalArgumentException("请填写 JDBC 驱动类");
            }
            conn.setBaseUrl(conn.getBaseUrl().trim());
        } else {
            validateAllowIps(conn.getAllowIps());
        }
    }

    private void validateEndpoint(GwEndpoint endpoint) {
        if (endpoint.getConnectionId() == null || endpoint.getConnectionId().isBlank()) {
            throw new IllegalArgumentException("缺少所属连接");
        }
        GwConnection conn = connectionRepository.findById(endpoint.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("所属连接不存在"));
        if (endpoint.getName() == null || endpoint.getName().isBlank()) {
            throw new IllegalArgumentException("请填写端点名称");
        }
        if (endpoint.getEndpointCode() == null || !endpoint.getEndpointCode().trim().matches("[A-Za-z][A-Za-z0-9_]{0,49}")) {
            throw new IllegalArgumentException("端点编码用字母开头，仅含字母、数字、下划线");
        }
        endpoint.setEndpointCode(endpoint.getEndpointCode().trim());
        for (GwEndpoint other : endpointRepository.findAll()) {
            if (endpoint.getEndpointCode().equalsIgnoreCase(other.getEndpointCode())
                    && !other.getId().equals(endpoint.getId())) {
                throw new IllegalArgumentException("端点编码已存在：" + endpoint.getEndpointCode());
            }
        }
        String method = endpoint.getMethod() == null ? "" : endpoint.getMethod().trim().toUpperCase();
        String type = conn.getType() == null ? "" : conn.getType().toUpperCase();
        if ("WEBHOOK".equals(type)) {
            if (!"WEBHOOK".equals(method)) {
                throw new IllegalArgumentException("被动接收连接的端点方式只能是 WEBHOOK");
            }
        } else if ("JDBC".equals(type)) {
            if (!"POLLING".equals(method)) {
                throw new IllegalArgumentException("JDBC 连接的端点方式只能是 POLLING");
            }
        } else if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("HTTP 连接的端点方式只能是 GET 或 POST");
        }
        endpoint.setMethod(method);
        if ("POLLING".equals(method)) {
            if (endpoint.getCronExpression() == null || endpoint.getCronExpression().isBlank()
                    || !CronExpression.isValidExpression(endpoint.getCronExpression().trim())) {
                throw new IllegalArgumentException("POLLING 需要合法的 Cron，例如 0/30 * * * * ?");
            }
            endpoint.setCronExpression(endpoint.getCronExpression().trim());
            assertSelectOnly(endpoint.getPath());
        }
        if ("GET".equals(method) || "POST".equals(method)) {
            if (Integer.valueOf(1).equals(endpoint.getEnvMode())) {
                if (endpoint.getMockPath() == null || endpoint.getMockPath().isBlank()) {
                    throw new IllegalArgumentException("测试环境需要填写 Mock 路径");
                }
            } else if (endpoint.getPath() == null || endpoint.getPath().isBlank()) {
                throw new IllegalArgumentException("请填写请求路径");
            }
        }
        if (endpoint.getStatus() != null && endpoint.getStatus() != 0 && endpoint.getStatus() != 1) {
            throw new IllegalArgumentException("状态只能是启用或停用");
        }
    }

    private void validateMapping(GwMappingRule rule) {
        if (rule.getEndpointId() == null || endpointRepository.findById(rule.getEndpointId()).isEmpty()) {
            throw new IllegalArgumentException("映射所属端点不存在");
        }
        MappingEval.checkExpression(rule.getRuleExpression());
        boolean constant = rule.getRuleExpression() != null && rule.getRuleExpression().trim().regionMatches(true, 0, "CONST:", 0, 6);
        if (!constant && (rule.getSourceField() == null || rule.getSourceField().isBlank())) {
            throw new IllegalArgumentException("请填写源字段");
        }
        if (rule.getTargetField() == null || rule.getTargetField().isBlank()) {
            throw new IllegalArgumentException("请填写目标字段");
        }
        if (!constant && rule.getSourceField().trim().startsWith("$")) {
            try {
                JsonPath.compile(rule.getSourceField().trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("JSONPath 不合法：" + e.getMessage());
            }
        }
        if (constant && (rule.getSourceField() == null || rule.getSourceField().isBlank())) {
            rule.setSourceField("-");
        }
    }

    private void assertSelectOnly(String sql) {
        if (sql == null || !sql.trim().regionMatches(true, 0, "SELECT", 0, 6)) {
            throw new IllegalArgumentException("JDBC 端点只允许一条 SELECT");
        }
        String upper = sql.toUpperCase();
        if (upper.contains(";") || upper.contains("INSERT ") || upper.contains("UPDATE ")
                || upper.contains("DELETE ") || upper.contains("DROP ") || upper.contains("ALTER ")) {
            throw new IllegalArgumentException("JDBC 端点只允许一条 SELECT");
        }
    }

    private void validateAllowIps(String allowIps) {
        if (allowIps == null || allowIps.isBlank()) {
            return;
        }
        for (String token : allowIps.split("[,;\\s]+")) {
            if (token.isBlank() || "*".equals(token)) {
                continue;
            }
            boolean ipv4 = token.matches("\\d{1,3}(\\.\\d{1,3}){3}");
            boolean ipv6 = token.contains(":");
            if (!ipv4 && !ipv6) {
                throw new IllegalArgumentException("白名单 IP 格式不正确：" + token);
            }
        }
    }
}
