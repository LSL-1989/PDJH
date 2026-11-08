package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.ScreenDeptMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenDeptMappingRepository extends JpaRepository<ScreenDeptMapping, Integer> {

    List<ScreenDeptMapping> findByScreenId(String screenId);

    List<ScreenDeptMapping> findByDeptId(String deptId);

    void deleteByScreenId(String screenId);

    void deleteByDeptId(String deptId);
}
