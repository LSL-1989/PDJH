package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.ExamRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRecordDetailRepository extends JpaRepository<ExamRecordDetail, Integer> {
    
    // 找出某患者尚未完成及未排队的项目 (state = 0)
    List<ExamRecordDetail> findByPatientCodeAndState(String patientCode, Integer state);
    
    // 获取某个患者当天需要进行的所有体检项目清单
    List<ExamRecordDetail> findByPatientCode(String patientCode);
}
