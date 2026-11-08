package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.PendingIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingIssueRepository extends JpaRepository<PendingIssue, Long> {

    List<PendingIssue> findByStatusOrderByCreateDateDesc(Integer status);

    Optional<PendingIssue> findFirstByIssueTypeAndRefIdAndStatus(String issueType, String refId, Integer status);

    long countByStatus(Integer status);
}
