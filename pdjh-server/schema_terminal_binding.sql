-- 终端双线绑定：doc_screen 扩展 + device_code 唯一约束
-- 若已开启 spring.jpa.hibernate.ddl-auto=update，通常由 Hibernate 同步列；生产环境建议手工执行。

ALTER TABLE doc_screen
    ADD COLUMN IF NOT EXISTS terminal_type VARCHAR(20) NULL COMMENT 'SCREEN/CLINIC',
    ADD COLUMN IF NOT EXISTS template_route VARCHAR(50) NULL COMMENT '候诊模板路由如 /pdjh1';

-- state 语义：0=冻结 1=启用 2=待配置（应用层约定，列类型不变）
ALTER TABLE doc_screen
    ADD COLUMN IF NOT EXISTS last_seen_at DATETIME NULL COMMENT '终端最近 Register/Resolve 时间';

-- device_code 唯一（允许多条 NULL）
CREATE UNIQUE INDEX IF NOT EXISTS uk_docscreen_devicecode ON doc_screen (device_code);

ALTER TABLE sign_device
    ADD COLUMN IF NOT EXISTS last_seen_at DATETIME NULL COMMENT '终端最近 Register/Resolve/GetDeviceConfig 时间';
