package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.ImplementTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImplementTaskRepository extends JpaRepository<ImplementTask, Long> {
    ImplementTask findByStepCode(String stepCode);
}
