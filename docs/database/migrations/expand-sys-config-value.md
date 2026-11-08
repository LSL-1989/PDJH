# 扩展 sys_config.config_value 字段

## 背景

「大屏显示内容配置」将整份 JSON 存入 `sys_config.config_value`（键名 `SCREEN_CFG_{屏幕ID}`）。
原表字段为 `varchar(255)`，默认配置 JSON 约 420+ 字符，保存时 MySQL 截断/报错，前端表现为「保存失败」500。

## 修复

```sql
ALTER TABLE sys_config
  MODIFY COLUMN config_value TEXT NULL,
  MODIFY COLUMN description TEXT NULL;
```

## 验证

```powershell
# 保存应返回 code=200
Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/SystemConfig/SaveConfig" `
  -Method POST -ContentType "application/json" `
  -Body '{"configKey":"SCREEN_CFG_test","configValue":"{\"layout\":\"2COL\",\"fontSize\":32}","description":"test"}'
```

后台「大屏显示内容配置」页选中设备后点保存，应提示成功且再次选中可回显。
