package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByScheduleDateBetweenOrderByDoctorIdAscScheduleDateAsc(LocalDate start, LocalDate end);

    List<DoctorSchedule> findByScheduleDateBetweenAndDeptIdOrderByDoctorIdAscScheduleDateAsc(
            LocalDate start, LocalDate end, String deptId);

    @Transactional
    void deleteByDoctorIdAndScheduleDate(String doctorId, LocalDate scheduleDate);

    @Transactional
    void deleteByDeptId(String deptId);

    @Transactional
    void deleteByDoctorId(String doctorId);

    List<DoctorSchedule> findByDoctorId(String doctorId);

    List<DoctorSchedule> findByOfficeRoomId(String officeRoomId);

    List<DoctorSchedule> findByDoctorIdAndScheduleDate(String doctorId, java.time.LocalDate scheduleDate);

    boolean existsByDoctorId(String doctorId);

    boolean existsByShiftType(String shiftType);

    /** 曾经排过班的医生，供排班列表和下拉；与按周查明细不是同一范围 */
    @Query("select distinct s.doctorId from DoctorSchedule s where s.doctorId is not null and s.doctorId <> ''")
    List<String> findDistinctDoctorIds();
}
