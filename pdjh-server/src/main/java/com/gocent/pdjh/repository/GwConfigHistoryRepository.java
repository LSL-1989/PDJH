package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.GwConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GwConfigHistoryRepository extends JpaRepository<GwConfigHistory, Long> {

    List<GwConfigHistory> findByEntityTypeAndEntityIdOrderByCreateTimeDesc(String entityType, String entityId);
}
