-- 护士当班排班（T-19，§16.1）
-- 若已开启 spring.jpa.hibernate.ddl-auto=update，通常由 Hibernate 自动建表；生产可手工执行。

CREATE TABLE IF NOT EXISTS nurse_schedule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_no VARCHAR(50) NOT NULL COMMENT '护士工号，关联 users.user_no',
  dept_id VARCHAR(64) NOT NULL COMMENT '当班诊区/科室',
  schedule_date DATE NOT NULL COMMENT '排班日',
  shift_type VARCHAR(16) NOT NULL COMMENT 'AM/PM/FULL',
  create_time DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_nurse_sched_user_date (user_no, schedule_date)
) COMMENT='护士当班排班';
