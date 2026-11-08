package com.gocent.pdjh.integration.webhook;

import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.entity.GwMappingRule;
import com.gocent.pdjh.integration.engine.MappingEval;
import com.gocent.pdjh.repository.GwEndpointRepository;
import com.gocent.pdjh.repository.GwMappingRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebhookDispatcherService {

    @Autowired
    private GwEndpointRepository endpointRepository;

    @Autowired
    private GwMappingRuleRepository mappingRuleRepository;

    /**
     * Parse the incoming webhook payload based on the mapping rules of the designated endpoint.
     * @param hookId Matches the endpointCode
     * @param payload Raw JSON string
     * @return Normalized list of extracted maps
     */
    public List<Map<String, Object>> dispatchAndParse(String hookId, String payload) {
        
        List<GwEndpoint> endpoints = endpointRepository.findAll();
        GwEndpoint endpoint = endpoints.stream()
                .filter(e -> hookId.equalsIgnoreCase(e.getEndpointCode()))
                .findFirst()
                .orElse(null);

        if (endpoint == null || endpoint.getStatus() == 0) {
            throw new IllegalArgumentException("Invalid or disabled webhook hookId: " + hookId);
        }

        List<GwMappingRule> rules = mappingRuleRepository.findByEndpointId(endpoint.getId());
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return MappingEval.fromJson(payload, rules);
    }
}
