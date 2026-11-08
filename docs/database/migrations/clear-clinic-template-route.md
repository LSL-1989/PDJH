# 迁移：清理 CLINIC 设备的 template_route

> **背景：** 门边屏（CLINIC）由 `HJ诊室门边屏终端`（`/clinic/`）渲染，不再使用候诊大屏 pdjh 模板路由。  
> **脚本：** [clear-clinic-template-route.sql](./clear-clinic-template-route.sql)

## 何时执行

- 升级包含「显示屏终端分层」改造的版本后
- 发现 `doc_screen.terminal_type='CLINIC'` 且 `template_route` 非空的历史数据

## 执行方式

```bash
mysql -u root -p pdjh < docs/database/migrations/clear-clinic-template-route.sql
```

## 验证

```sql
SELECT id, name, terminal_type, template_route
FROM doc_screen
WHERE terminal_type = 'CLINIC';
```

预期：`template_route` 均为 NULL。
