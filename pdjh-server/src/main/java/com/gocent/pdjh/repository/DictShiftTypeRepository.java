package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DictShiftType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 医生出诊班次字典仓储 */
public interface DictShiftTypeRepository extends JpaRepository<DictShiftType, Integer> {

    List<DictShiftType> findAllByOrderBySortOrderAscIdAsc();

    Optional<DictShiftType> findByCode(String code);

    Optional<DictShiftType> findByName(String name);
}
