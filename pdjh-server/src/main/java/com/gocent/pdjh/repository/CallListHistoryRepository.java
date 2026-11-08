package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.CallListHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallListHistoryRepository extends JpaRepository<CallListHistory, Integer> {
    
}
