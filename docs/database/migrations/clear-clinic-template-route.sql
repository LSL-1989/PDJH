-- 清理 CLINIC 门边屏设备上遗留的 pdjh 模板路由（legacy pdjh2/pdjh9）
-- 执行前请备份 doc_screen 表
-- 适用：MySQL 8+ / pdjh 库

UPDATE doc_screen
SET template_route = NULL
WHERE terminal_type = 'CLINIC'
  AND template_route IS NOT NULL
  AND template_route <> '';

-- 可选：仅清理明确 legacy 路由
-- UPDATE doc_screen
-- SET template_route = NULL
-- WHERE terminal_type = 'CLINIC'
--   AND template_route IN ('/pdjh2', '/pdjh9');
