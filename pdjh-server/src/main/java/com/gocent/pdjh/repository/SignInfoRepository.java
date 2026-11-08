package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SignInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignInfoRepository extends JpaRepository<SignInfo, String> {
    Optional<SignInfo> findByCode(String code);
}
