package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.PatientFlowLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientFlowLogRepository extends JpaRepository<PatientFlowLog, Long>,
        JpaSpecificationExecutor<PatientFlowLog> {

    List<PatientFlowLog> findByCallListIdOrderByCreateTimeAsc(Integer callListId);

    List<PatientFlowLog> findByPatientCodeOrderByCreateTimeAsc(String patientCode);
}
