package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.NurseActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NurseActionLogRepository extends JpaRepository<NurseActionLog, Long> {

    Page<NurseActionLog> findByZoneIdOrderByCreateTimeDesc(String zoneId, Pageable pageable);
}
