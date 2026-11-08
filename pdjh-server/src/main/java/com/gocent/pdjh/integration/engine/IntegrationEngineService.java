package com.gocent.pdjh.integration.engine;

import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.repository.GwConnectionRepository;
import com.gocent.pdjh.repository.GwEndpointRepository;
import com.gocent.pdjh.repository.GwMappingRuleRepository;
import com.gocent.pdjh.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IntegrationEngineService {

    @Autowired
    private GwConnectionRepository connectionRepository;

    @Autowired
    private GwEndpointRepository endpointRepository;

    @Autowired
    private GwMappingRuleRepository mappingRuleRepository;

    @Autowired
    private HttpIntegrationExecutor httpExecutor;

    @Autowired
    private JdbcIntegrationExecutor jdbcExecutor;

    @Autowired
    private CircuitBreakerManager circuitBreaker;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Call this from business logic (e.g. Queue Registration API).
     * @param endpointCode the universal configuration code
     * @param params dynamic contextual inputs like patientCode
     * @return the correctly mapped target data payload
     */
    public List<Map<String, Object>> executeGatewayCall(String endpointCode, Map<String, Object> params) {
        
        // 1. Fetch Endpoint info
        List<GwEndpoint> endpoints = endpointRepository.findAll();
        GwEndpoint endpoint = endpoints.stream()
                .filter(e -> endpointCode.equalsIgnoreCase(e.getEndpointCode()))
                .findFirst()
                .orElse(null);

        if (endpoint == null || endpoint.getStatus() == 0) {
            throw new IllegalArgumentException("Endpoint configuration not found for: " + endpointCode);
        }

        // 2. Fetch Connection info
        GwConnection connection = connectionRepository.findById(endpoint.getConnectionId()).orElse(null);
        if (connection == null || connection.getStatus() == 0) {
            throw new IllegalArgumentException("Connection not found for endpoint: " + endpointCode);
        }

        // 3. Fetch Mapping Rules
        List<GwMappingRule> rules = mappingRuleRepository.findByEndpointId(endpoint.getId());

        // Select the strategy
        IntegrationExecutor strategy;
        if ("HTTP".equalsIgnoreCase(connection.getType())) {
            strategy = httpExecutor;
        } else if ("JDBC".equalsIgnoreCase(connection.getType())) {
            strategy = jdbcExecutor;
        } else {
            throw new UnsupportedOperationException("Unsupported connection type: " + connection.getType());
        }

        long startTime = System.currentTimeMillis();
        boolean isSuccess = false;
        String errorMsg = null;
        List<Map<String, Object>> result = null;

        try {
            // Check Circuit Breaker before executing
            circuitBreaker.checkAndThrow(endpointCode);

            // Execute the remote call and mapping
            result = strategy.execute(connection, endpoint, rules, params);
            
            // Record success for the breaker
            circuitBreaker.recordSuccess(endpointCode);
            isSuccess = true;
            return result;
        } catch (Exception e) {
            circuitBreaker.recordFailure(endpointCode);
            errorMsg = e.getMessage();
            throw new RuntimeException("Integration execution failed for " + endpointCode, e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // Async log to DB
            String paramStr = params != null ? params.toString() : "";
            // Keep response str short to not overload audit db for now
            String resStr = result != null ? result.toString() : "[]";
            auditLogService.logAsync(endpointCode, connection.getId(), paramStr, resStr, isSuccess, duration, errorMsg);
        }
    }
}
