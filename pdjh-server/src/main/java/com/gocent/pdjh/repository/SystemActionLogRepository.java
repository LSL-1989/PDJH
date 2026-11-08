package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.SystemActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface SystemActionLogRepository extends JpaRepository<SystemActionLog, Long> {

    // 分页查询：根据 logType 精确过滤，和 operator/actionDesc 模糊过滤
    @Query("SELECT s FROM SystemActionLog s WHERE " +
           "(:logType IS NULL OR s.logType = :logType) AND " +
           "(:keyword IS NULL OR (s.operator LIKE %:keyword% OR s.actionDesc LIKE %:keyword%)) AND " +
           "(:startDate IS NULL OR s.createTime >= :startDate) AND " +
           "(:endDate IS NULL OR s.createTime <= :endDate)")
    Page<SystemActionLog> searchLogs(String logType, String keyword, Date startDate, Date endDate, Pageable pageable);

    // 删除早于指定时间的陈旧日志
    @Modifying
    @Transactional
    @Query("DELETE FROM SystemActionLog s WHERE s.createTime < :cutoff")
    void deleteOlderThan(Date cutoff);
}
