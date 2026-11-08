-- 排队叫号系统索引优化（列名与 JPA 实体 / SCHEMA_NAMING.md 一致，均为下划线）
-- 前置：表已由 Hibernate ddl-auto=update 创建；重复执行若索引已存在会报 Duplicate key name

-- call_list：dept_id, state, is_forzen, call_date
CREATE INDEX idx_calllist_dept_state_date
  ON call_list (dept_id, state, is_forzen, call_date);

-- doc_screen：设备码路由查询
CREATE INDEX idx_docscreen_devicecode ON doc_screen (device_code);

-- call_list：物理诊室区域校验与大屏过滤
CREATE INDEX idx_calllist_office_room ON call_list (office_room_id);

-- screen_dept_mapping（勿写成 screen_dept_map）
CREATE INDEX idx_screen_map_screenid ON screen_dept_mapping (screen_id);
CREATE INDEX idx_screen_map_deptid ON screen_dept_mapping (dept_id);

-- nurse_action_log：护士站干预审计（JPA ddl-auto=update 亦可自动建表）
CREATE INDEX idx_nurse_log_zone_time ON nurse_action_log (zone_id, create_time);
CREATE INDEX idx_nurse_log_call_list ON nurse_action_log (call_list_id);
