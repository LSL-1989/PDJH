package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.service.IntegrationConfigService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源接口配置：连接、端点、映射、试跑与回滚。
 * 消费者：管理后台「数据源接口配置」。
 * 依赖：IntegrationConfigService 与 gw_* 表。
 */
@RestController
@RequestMapping("/integration/config")
public class IntegrationConfigController {

    @Autowired
    private IntegrationConfigService configService;

    @GetMapping("/connections")
    public ResultData<List<GwConnection>> getConnections() {
        return ResultData.success(configService.getAllConnections());
    }

    @PostMapping("/connection")
    public ResultData<GwConnection> saveConnection(@RequestBody GwConnection conn) {
        try {
            GwConnection saved = configService.saveConnection(conn);
            configService.present(saved);
            return ResultData.success(saved);
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        }
    }

    @DeleteMapping("/connection/{id}")
    public ResultData<Void> deleteConnection(@PathVariable String id) {
        try {
            configService.deleteConnection(id);
            return ResultData.success(null, "Connection deleted");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        }
    }

    @GetMapping("/connection/{id}/ping")
    public ResultData<Boolean> pingConnection(@PathVariable String id) {
        try {
            return ResultData.success(true, configService.pingConnection(id));
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        }
    }

    @GetMapping("/endpoints/{connectionId}")
    public ResultData<List<GwEndpoint>> getEndpoints(@PathVariable String connectionId) {
        return ResultData.success(configService.getEndpointsByConnection(connectionId));
    }

    @PostMapping("/endpoint")
    public ResultData<GwEndpoint> saveEndpoint(@RequestBody GwEndpoint endpoint,
                                               @RequestHeader(value = "X-User-No", required = false) String operator) {
        try {
            return ResultData.success(configService.saveEndpoint(endpoint, operator));
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        }
    }

    @DeleteMapping("/endpoint/{id}")
    public ResultData<Void> deleteEndpoint(@PathVariable String id,
                                           @RequestHeader(value = "X-User-No", required = false) String operator) {
        configService.deleteEndpoint(id, operator);
        return ResultData.success(null, "Endpoint deleted");
    }

    @GetMapping("/mappings/{endpointId}")
    public ResultData<List<GwMappingRule>> getMappings(@PathVariable String endpointId) {
        return ResultData.success(configService.getMappingRulesByEndpoint(endpointId));
    }

    @PostMapping("/mapping")
    public ResultData<GwMappingRule> saveMappingRule(@RequestBody GwMappingRule rule,
                                                     @RequestHeader(value = "X-User-No", required = false) String operator) {
        try {
            return ResultData.success(configService.saveMappingRule(rule, operator));
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        }
    }

    @DeleteMapping("/mapping/{id}")
    public ResultData<Void> deleteMappingRule(@PathVariable String id,
                                              @RequestHeader(value = "X-User-No", required = false) String operator) {
        configService.deleteMappingRule(id, operator);
        return ResultData.success(null, "Mapping rule deleted");
    }

    @GetMapping("/history/endpoint/{endpointId}")
    public ResultData<List<com.gocent.pdjh.entity.GwConfigHistory>> getEndpointHistory(@PathVariable String endpointId) {
        return ResultData.success(configService.getEndpointHistory(endpointId));
    }

    @PostMapping("/history/rollback/{historyId}")
    public ResultData<Void> rollbackEndpoint(@PathVariable Long historyId,
                                             @RequestHeader(value = "X-User-No", required = false) String operator) {
        try {
            configService.rollbackEndpoint(historyId, operator);
            return ResultData.success(null, "Rollback successful");
        } catch (Exception e) {
            return ResultData.fail("Rollback failed: " + e.getMessage());
        }
    }

    /**
     * 试跑映射。传 endpointId 时按该端点已保存的规则整表提取；
     * 只传 mockData + sourceField 时仍按单条 JSONPath 提取。
     */
    @PostMapping("/debug/dry-run")
    public ResultData<Object> dryRunMapping(@RequestBody Map<String, String> request) {
        String endpointId = request.get("endpointId");
        String mockData = request.get("mockData");
        String sourceField = request.get("sourceField");
        try {
            if (endpointId != null && !endpointId.isBlank()) {
                return ResultData.success(configService.dryRun(endpointId, mockData), "Extraction successful");
            }
            if (mockData == null || sourceField == null) {
                return ResultData.fail("mockData and sourceField are required");
            }
            Object result = JsonPath.read(mockData, sourceField);
            return ResultData.success(result, "Extraction successful");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(e.getMessage());
        } catch (Exception e) {
            return ResultData.fail("Extraction failed: " + e.getMessage());
        }
    }
}
