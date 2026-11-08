package com.gocent.pdjh.integration.engine;

import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.integration.engine.MappingEval;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JdbcIntegrationExecutor implements IntegrationExecutor {

    @Override
    public boolean supports(String type) {
        return "JDBC".equalsIgnoreCase(type);
    }

    @Override
    public List<Map<String, Object>> execute(GwConnection connection, GwEndpoint endpoint, List<GwMappingRule> rules, Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Load driver lazily
        try {
            Class.forName(connection.getDriverClass());
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + connection.getDriverClass());
            return results;
        }

        // endpoint.getPath() contains the raw SQL in JDBC mode
        String sql = endpoint.getPath();
        
        // Simple Named Parameter replacement (Very basic)
        // e.g., SELECT * FROM HIS_VIEW WHERE PATIENT_CODE = {patientCode}
        // In reality, should use PreparedStatement securely
        for (Map.Entry<String, Object> param : params.entrySet()) {
            sql = sql.replace("{" + param.getKey() + "}", "'" + param.getValue() + "'");
        }

        try (Connection con = DriverManager.getConnection(connection.getBaseUrl(), connection.getUsername(), connection.getPassword());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (GwMappingRule rule : rules) {
                    try {
                        if (rule.getRuleExpression() != null && rule.getRuleExpression().trim().regionMatches(true, 0, "CONST:", 0, 6)) {
                            row.put(rule.getTargetField(), MappingEval.mapCell(null, rule));
                            continue;
                        }
                        Object val = rs.getObject(rule.getSourceField());
                        row.put(rule.getTargetField(), MappingEval.mapCell(val, rule));
                    } catch (SQLException ignore) {
                    }
                }
                results.add(row);
            }

        } catch (SQLException e) {
            System.err.println("JDBC Execution failed: " + e.getMessage());
        }

        return results;
    }
}
