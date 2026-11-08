package com.gocent.pdjh.integration.engine;

import com.gocent.pdjh.entity.GwConnection;
import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.integration.engine.MappingEval;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HttpIntegrationExecutor implements IntegrationExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Override
    public boolean supports(String type) {
        return "HTTP".equalsIgnoreCase(type);
    }

    @Override
    public List<Map<String, Object>> execute(GwConnection connection, GwEndpoint endpoint, List<GwMappingRule> rules, Map<String, Object> params) {
        // 1. Build URL depending on Environment Mode
        String url;
        if (endpoint.getEnvMode() != null && endpoint.getEnvMode() == 1 && endpoint.getMockPath() != null) {
            url = resolveMockUrl(endpoint.getMockPath());
        } else {
            url = connection.getBaseUrl() + endpoint.getPath();
        }
        
        // Very basic param replace in URL if needed (e.g. /api/user?code={patientCode})
        for (Map.Entry<String, Object> param : params.entrySet()) {
            url = url.replace("{" + param.getKey() + "}", String.valueOf(param.getValue()));
        }

        // 2. Build Headers
        HttpHeaders headers = new HttpHeaders();
        // If username/password is used as a Bearer token or basic auth, attach it here
        if (connection.getUsername() != null && !connection.getUsername().isEmpty()) {
            headers.set("Authorization", connection.getUsername()); 
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. Make HTTP Request
        String methodName = endpoint.getMethod() == null ? "GET" : endpoint.getMethod().toUpperCase();
        if ("POLLING".equals(methodName) || "WEBHOOK".equals(methodName)) {
            methodName = "GET";
        }
        HttpMethod method = HttpMethod.valueOf(methodName);
        ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
        String jsonPayload = response.getBody();

        // 4. Parse JSON using Mapping Rules
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Typically, we assume the payload is an array of items, or we extract an array first.
        // For simplicity in this engine core, we do a flat map evaluation or rely on the JSON path returning a List.
        // If a rule evaluates to a list, we fan it out. 
        if (jsonPayload != null && rules != null && !rules.isEmpty()) {
            return MappingEval.fromJson(jsonPayload, rules);
        }
        return results;
    }

    private String resolveMockUrl(String mockPath) {
        if (mockPath.startsWith("http://") || mockPath.startsWith("https://")) {
            return mockPath;
        }
        String path = mockPath.startsWith("/") ? mockPath : "/" + mockPath;
        String ctx = contextPath == null ? "" : contextPath.trim();
        if (!ctx.isEmpty() && !ctx.startsWith("/")) {
            ctx = "/" + ctx;
        }
        if (ctx.endsWith("/")) {
            ctx = ctx.substring(0, ctx.length() - 1);
        }
        if (!ctx.isEmpty() && (path.equals(ctx) || path.startsWith(ctx + "/"))) {
            return "http://127.0.0.1:" + serverPort + path;
        }
        return "http://127.0.0.1:" + serverPort + ctx + path;
    }
}
