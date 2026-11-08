package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.MedTechSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedTechSlotRepository extends JpaRepository<MedTechSlot, Long> {

    List<MedTechSlot> findByDeptId(String deptId);

    /** 查询指定科室指定日期的所有时段 */
    List<MedTechSlot> findByDeptIdAndSlotDateOrderByTimeSlotAsc(String deptId, LocalDate slotDate);

    /** 查询指定科室、日期、时段——预约时去重判断 */
    Optional<MedTechSlot> findByDeptIdAndSlotDateAndTimeSlot(String deptId, LocalDate slotDate, String timeSlot);

    /** 查询某科室某日期仍有余量的时段（booked < capacity 且 isDisabled=0） */
    @Query("SELECT s FROM MedTechSlot s WHERE s.deptId = :deptId " +
           "AND s.slotDate = :date AND s.isDisabled = 0 AND s.booked < s.capacity " +
           "ORDER BY s.timeSlot ASC")
    List<MedTechSlot> findAvailableSlots(@Param("deptId") String deptId,
                                          @Param("date") LocalDate date);

    /** 原子自增 booked（预约成功后调用） */
    @Modifying
    @Transactional
    @Query("UPDATE MedTechSlot s SET s.booked = s.booked + 1 " +
           "WHERE s.id = :id AND s.booked < s.capacity")
    int incrementBooked(@Param("id") Long id);

    /** 原子自减 booked（取消预约后调用） */
    @Modifying
    @Transactional
    @Query("UPDATE MedTechSlot s SET s.booked = GREATEST(s.booked - 1, 0) WHERE s.id = :id")
    int decrementBooked(@Param("id") Long id);
}
