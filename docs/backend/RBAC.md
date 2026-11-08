# 后台 RBAC 配置与登录说明

> **文档位置：** `docs/backend/` · 菜单/角色/权限变更请同步更新本文档。

## 配置顺序

1. **菜单管理**（`sys_menu`）  
   维护侧栏路由；首次启动时 `SysMenuSeedRunner` 会写入最小菜单树，也可在「菜单管理」中增删。

2. **角色管理**（`sys_role`）  
   创建角色，在权限树中勾选菜单后「保存权限」，写入 `sys_role_menu`（`role_id` + `menu_id`）。

3. **用户管理**（`users`）  
   创建用户时必须选择角色；`users.role_id` 关联 `sys_role.role_id`，`users.role` 同步为 `role_code`（列表展示用）。

4. **登录**（`/api/User/LoginUser`）  
   校验账号密码 → 用户启用 → 角色存在且未禁用 → 角色至少有一条 `sys_role_menu` → 按授权组装 `Routes`。

## 表关系

```
sys_menu  ←── sys_role_menu ──→  sys_role
                                      ↑
                                 users.role_id
```

## 启动种子

`AdminBootstrapRunner`（`@Order(101)`）在菜单种子之后执行：

- 若无「系统管理员」角色：创建 `role_id=admin`、`role_code=ROLE_ADMIN`
- 将当前全部未禁用菜单授权给该角色
- 若无 `wuaitecadmin`：建表后由启动器写入默认超管，密码 BCrypt 入库（明文见 `ADMIN_DEFAULT_PASSWORD`，不写 SQL）
- 若 `wuaitecadmin` 已存在但 `role_id` 为空：自动补绑管理员角色
- 启动时将 users 表中遗留明文密码升级为 BCrypt

## 测试数据

```bash
mysql -h localhost -u root -p pdjh < scripts/seed_pdjh_test_data.sql
```

- `wuaitecadmin` / `wuaitec@123`：固定超管，全部 SEED 菜单  
- `doctor01` / `Doctor@2026`：仅 `SEED_M010` 首页

## 常见登录失败提示

| 提示 | 处理 |
|------|------|
| 未分配角色 | 用户管理中选择角色 |
| 角色未配置任何菜单权限 | 角色管理中勾选菜单并保存 |
| 角色已禁用 | 启用角色或更换角色 |
| 账号已禁用 | 将用户状态设为启用 |

## 说明

- `sys_menu.role_codes` 已不作为登录菜单来源；权限以 `sys_role_menu` 为准。  
- API 层仍为 `permitAll`（除 `/Integration/**`）；鉴权依赖前端路由与业务 Token，全接口拦截不在本方案范围。
