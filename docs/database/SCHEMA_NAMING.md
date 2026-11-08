# 数据库表名与列名约定

> **文档位置：** `docs/database/` · 表结构或命名变更请同步更新 [数据库设计说明书](数据库设计说明书.md) 与本文件。

与 `pdjh-server` JPA 实体及原生 SQL 保持一致：

| 层级 | 规则 | 示例 |
|------|------|------|
| 表名 | 小写 + 下划线 | `call_list`, `screen_dept_mapping` |
| 列名 | 小写 + 下划线（Spring Boot 默认物理命名） | `dept_id`, `is_forzen`, `call_date` |
| Java 字段 | 驼峰；无 `@Column` 时由 Hibernate 映射为下划线列名 | `deptID` → `dept_id` |

## 权威来源

1. **建表**：`spring.jpa.hibernate.ddl-auto=update`，以 `entity/*.java` 为准。
2. **手工 DDL**：`schema_admin_gap.sql`（仅 ddl-auto 关闭时）。
3. **索引**：`schema_optimizations.sql`（列名必须用下划线，不可用 Java 驼峰）。
4. **测试数据**：`scripts/seed_pdjh_test_data.sql`。

## 已废弃（勿用于当前后端）

- `src/main/resources/schema.sql` — 早期原型表，与现有实体不一致。
- 根目录 `mock_db_init.sql` — 旧字段名，请改用 `scripts/seed_pdjh_test_data.sql`。

权限与登录流程见 [RBAC.md](../backend/RBAC.md)。

## 易错对照（call_list）

| 错误（脚本/旧写法） | 正确（MySQL 列名） |
|--------------------|-------------------|
| `deptID` | `dept_id` |
| `isForzen` | `is_forzen` |
| `callDate` | `call_date` |
| `screen_dept_map` | `screen_dept_mapping` |
