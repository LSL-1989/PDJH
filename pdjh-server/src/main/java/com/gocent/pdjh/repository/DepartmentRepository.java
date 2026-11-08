package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    List<Department> findByFatherID(String fatherId);

    List<Department> findByBusinessTypeIn(Collection<String> businessTypes);

    List<Department> findByName(String name);

    boolean existsByName(String name);
}
