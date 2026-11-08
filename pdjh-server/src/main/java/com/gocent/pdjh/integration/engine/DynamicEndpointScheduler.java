package com.gocent.pdjh.integration.engine;

import com.gocent.pdjh.entity.GwEndpoint;
import com.gocent.pdjh.repository.GwEndpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class DynamicEndpointScheduler implements SchedulingConfigurer {

    @Autowired
    private GwEndpointRepository endpointRepository;

    @Autowired
    private IntegrationEngineService integrationEngineService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Fetch all polling endpoints
        List<GwEndpoint> endpoints = endpointRepository.findAll();
        
        for (GwEndpoint endpoint : endpoints) {
            // Only schedule if it's meant to be polled and not disabled
            if ("POLLING".equalsIgnoreCase(endpoint.getMethod()) 
                    && endpoint.getStatus() != null 
                    && endpoint.getStatus() == 1 
                    && endpoint.getCronExpression() != null 
                    && !endpoint.getCronExpression().trim().isEmpty()) {
                
                Runnable task = () -> {
                    System.out.println("Executing Scheduled Polling Endpoint: " + endpoint.getEndpointCode());
                    try {
                        List<Map<String, Object>> results = integrationEngineService.executeGatewayCall(endpoint.getEndpointCode(), Collections.emptyMap());
                        if (!results.isEmpty()) {
                            // Forward the results to the actual business logic / message bus
                            // e.g., PatientQueueEventBus.publish(endpoint.getEndpointCode(), results);
                            System.out.println("Polling fetched " + results.size() + " new records.");
                        }
                    } catch (Exception e) {
                        System.err.println("Scheduled Polling failed for " + endpoint.getEndpointCode() + ": " + e.getMessage());
                    }
                };
                
                CronTrigger trigger = new CronTrigger(endpoint.getCronExpression());
                taskRegistrar.addTriggerTask(task, trigger);
            }
        }
    }
}
