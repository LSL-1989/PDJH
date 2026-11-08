package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysConfigRepository extends JpaRepository<SysConfig, String> {
}
