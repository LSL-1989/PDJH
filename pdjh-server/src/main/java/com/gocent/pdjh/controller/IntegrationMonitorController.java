package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.repository.GwAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/integration/monitor")
public class IntegrationMonitorController {

    @Autowired
    private GwAuditLogRepository auditLogRepository;

    @GetMapping("/dashboard")
    public ResultData<Map<String, Object>> getDashboardStats() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Load aggregate statistics per endpoint
        List<Map<String, Object>> stats = auditLogRepository.getOdometerStats();
        dashboard.put("endpointStats", stats);
        
        // Load recent errors for the alert list
        List<Map<String, Object>> errors = auditLogRepository.getRecentErrors();
        dashboard.put("recentErrors", errors);
        
        // Total logs count roughly indicates traffic load
        long totalTraffic = auditLogRepository.count();
        dashboard.put("totalTraffic", totalTraffic);
        
        return ResultData.success(dashboard, "Monitor dashboard data loaded.");
    }
}
