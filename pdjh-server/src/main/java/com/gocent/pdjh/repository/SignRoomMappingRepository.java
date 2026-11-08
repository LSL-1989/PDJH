package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SignRoomMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignRoomMappingRepository extends JpaRepository<SignRoomMapping, Integer> {
    List<SignRoomMapping> findBySignDeviceId(String signDeviceId);
    void deleteBySignDeviceId(String signDeviceId);
    List<SignRoomMapping> findByOfficeRoomId(String officeRoomId);
}
