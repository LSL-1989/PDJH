package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.GwConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GwConnectionRepository extends JpaRepository<GwConnection, String> {
}
