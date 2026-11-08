package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.CallAutoNum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallAutoNumRepository extends JpaRepository<CallAutoNum, String> {

    Optional<CallAutoNum> findFirstByDidAndAutoDate(String did, LocalDate autoDate);

    List<CallAutoNum> findByDidAndAutoDate(String did, LocalDate autoDate);
}
