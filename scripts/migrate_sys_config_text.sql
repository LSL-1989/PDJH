-- 扩展 sys_config 值字段，支持 SCREEN_CFG_* 等大 JSON 配置
ALTER TABLE sys_config
  MODIFY COLUMN config_value TEXT NULL,
  MODIFY COLUMN description TEXT NULL;
