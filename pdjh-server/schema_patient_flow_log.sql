-- 患者全流程操作留痕表（ddl-auto=update 通常自动建表）
-- 覆盖：门诊签到/叫号/完成/复诊/过号、取药签到/叫号、护士干预等

CREATE TABLE IF NOT EXISTS patient_flow_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  call_list_id INT NULL,
  patient_code VARCHAR(64) NULL,
  patient_name VARCHAR(50) NULL,
  show_no VARCHAR(50) NULL,
  dept_id VARCHAR(64) NULL,
  event_type VARCHAR(40) NOT NULL,
  event_label VARCHAR(50) NULL,
  operator_source VARCHAR(20) NULL,
  operator_no VARCHAR(50) NULL,
  operator_name VARCHAR(50) NULL,
  remark VARCHAR(500) NULL,
  payload TEXT NULL,
  create_time DATETIME NULL,
  INDEX idx_pfl_call_list (call_list_id),
  INDEX idx_pfl_patient_time (patient_code, create_time),
  INDEX idx_pfl_event_time (event_type, create_time),
  INDEX idx_pfl_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
