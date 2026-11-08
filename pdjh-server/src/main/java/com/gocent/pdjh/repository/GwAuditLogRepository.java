package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.GwAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface GwAuditLogRepository extends JpaRepository<GwAuditLog, Long>, JpaSpecificationExecutor<GwAuditLog> {

    @Query(value = "SELECT endpoint_code as endpointCode, COUNT(*) as callCount, " +
                   "SUM(CASE WHEN is_success = true THEN 1 ELSE 0 END) as successCount, " +
                   "AVG(duration_ms) as avgDuration " +
                   "FROM gw_audit_log GROUP BY endpoint_code", nativeQuery = true)
    List<Map<String, Object>> getOdometerStats();
    
    @Query(value = "SELECT endpoint_code as endpointCode, error_message as error, create_time as time " +
                   "FROM gw_audit_log WHERE is_success = false ORDER BY create_time DESC LIMIT 50", nativeQuery = true)
    List<Map<String, Object>> getRecentErrors();
}
