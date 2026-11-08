package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DoctorDept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DoctorDeptRepository extends JpaRepository<DoctorDept, Long> {

    List<DoctorDept> findByDoctorId(String doctorId);

    List<DoctorDept> findByDoctorIdIn(Collection<String> doctorIds);

    List<DoctorDept> findByDeptId(String deptId);

    void deleteByDoctorId(String doctorId);

    void deleteByDoctorIdAndDeptId(String doctorId, String deptId);

    void deleteByDeptId(String deptId);

    boolean existsByDoctorIdAndDeptId(String doctorId, String deptId);
}
