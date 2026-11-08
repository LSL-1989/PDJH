package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DictDoctorTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 医生职称字典仓储 */
public interface DictDoctorTitleRepository extends JpaRepository<DictDoctorTitle, Integer> {

    List<DictDoctorTitle> findAllByOrderBySortOrderAscIdAsc();

    Optional<DictDoctorTitle> findByName(String name);
}
