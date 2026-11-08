package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SignDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignDeviceRepository extends JpaRepository<SignDevice, String> {
    Optional<SignDevice> findByDeviceCode(String deviceCode);
    List<SignDevice> findByNameContainingIgnoreCase(String name);
}
