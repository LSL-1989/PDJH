package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.NurseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 护士当班排班；消费者：NurseDeskServiceImpl、NurseScheduleController、删用户/科室时清理。
 */
@Repository
public interface NurseScheduleRepository extends JpaRepository<NurseSchedule, Long> {

    List<NurseSchedule> findByUserNoAndScheduleDateOrderByIdAsc(String userNo, LocalDate scheduleDate);

    List<NurseSchedule> findByScheduleDateOrderByUserNoAsc(LocalDate scheduleDate);

    /** 闭区间，供后台周视图一次取周一到周日 */
    List<NurseSchedule> findByScheduleDateBetweenOrderByScheduleDateAscUserNoAsc(LocalDate start, LocalDate end);

    List<NurseSchedule> findByUserNoOrderByScheduleDateDesc(String userNo);

    @Transactional
    void deleteByUserNoAndScheduleDate(String userNo, LocalDate scheduleDate);

    @Transactional
    void deleteByUserNo(String userNo);

    @Transactional
    void deleteByDeptId(String deptId);
}
