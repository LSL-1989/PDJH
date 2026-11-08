package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.DocScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocScreenRepository extends JpaRepository<DocScreen, String> {

    @Query("SELECT s FROM DocScreen s WHERE (:name IS NULL OR s.name LIKE %:name%)")
    List<DocScreen> findScreensByCondition(String name);

    Optional<DocScreen> findByDeviceCode(String deviceCode);
}
