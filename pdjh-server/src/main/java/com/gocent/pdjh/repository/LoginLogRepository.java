package com.gocent.pdjh.repository;

import com.gocent.pdjh.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * login_log 持久化。消费者：LoginLogService。
 */
@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}
