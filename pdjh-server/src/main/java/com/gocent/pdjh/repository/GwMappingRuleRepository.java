package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.GwMappingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GwMappingRuleRepository extends JpaRepository<GwMappingRule, String> {
    List<GwMappingRule> findByEndpointId(String endpointId);
}
