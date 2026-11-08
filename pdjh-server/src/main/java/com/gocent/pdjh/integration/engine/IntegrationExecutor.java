package com.gocent.pdjh.integration.engine;

import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;

import java.util.List;
import java.util.Map;

public interface IntegrationExecutor {
    
    /**
     * Define which connection type this executor supports (e.g., "HTTP", "JDBC").
     */
    boolean supports(String type);
    
    /**
     * Execute the endpoint call and map the results to a normalized list of maps.
     * @param connection The underlying connection info
     * @param endpoint The specific endpoint path/SQL
     * @param rules The mapped rules detailing how to extract data
     * @param params Incoming dynamic parameters (e.g., patientCode, date)
     * @return Normalized list of data records containing targetField keys
     */
    List<Map<String, Object>> execute(GwConnection connection, GwEndpoint endpoint, List<GwMappingRule> rules, Map<String, Object> params);
}
