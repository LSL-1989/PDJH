-- pdjh-server/schema_admin_gap.sql
-- 表名/列名下划线，与 entity 包一致；命名说明见 SCHEMA_NAMING.md / RBAC.md
-- 若已开启 spring.jpa.hibernate.ddl-auto=update，通常无需手工执行（由 Hibernate 建表）。
-- sys_role_menu 由实体 SysRoleMenu 自动建表；权限绑定见角色管理 CommitPermission 或 AdminBootstrapRunner。

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id VARCHAR(64) NOT NULL,
    menu_id VARCHAR(64) NOT NULL,
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 自动排号序列表（AutoNumManages / CallAutoNum）
CREATE TABLE IF NOT EXISTS call_auto_num (
    gid VARCHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(64) NULL,
    did VARCHAR(36) NOT NULL,
    name VARCHAR(200) NULL,
    auto_date DATE NOT NULL,
    type_a INT NOT NULL DEFAULT 0,
    type_b INT NOT NULL DEFAULT 0,
    type_c INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 医生主数据已统一为 doctor 表（含展示字段 remarks/display_status/avatar）
-- 自 screen_intro 迁移见 scripts/migrate_screen_intro_to_doctor.sql

-- 系统菜单（菜单管理 / 角色权限树数据源；与 JPA 实体 SysMenu 一致）
CREATE TABLE IF NOT EXISTS sys_menu (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    page_show_name VARCHAR(100) NULL,
    page_url VARCHAR(200) NULL,
    icon VARCHAR(100) NULL,
    level_id INT NULL,
    parent_page_id VARCHAR(64) NULL,
    always_show TINYINT(1) NULL,
    no_cache TINYINT(1) NULL,
    role_codes VARCHAR(200) NULL,
    sort_order INT NULL,
    is_delete TINYINT(1) NULL,
    redirect VARCHAR(200) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：与 SysMenuSeedRunner 相同的最小种子（仅当表为空时需执行；若已由服务启动写入可跳过）
INSERT IGNORE INTO sys_menu (id, page_show_name, page_url, icon, level_id, parent_page_id, always_show, no_cache, role_codes, sort_order, is_delete) VALUES
    ('00000000000000000000000000000001', '首页', 'dashboard', 'Home', 0, '0', 1, 0, NULL, 1, 0),
    ('00000000000000000000000000000002', '系统与权限管理', 'userManage', 'el-icon-s-operation', 0, '0', 1, 0, NULL, 2, 0),
    ('00000000000000000000000000000011', '用户管理', 'maintenanceUser', 'user', 1, '00000000000000000000000000000002', 1, 0, NULL, 11, 0),
    ('00000000000000000000000000000012', '角色管理', 'maintenanceRole', 'peoples', 1, '00000000000000000000000000000002', 1, 0, NULL, 12, 0),
    ('00000000000000000000000000000013', '菜单管理', 'maintenanceMenu', 'tree-table', 1, '00000000000000000000000000000002', 1, 0, NULL, 13, 0);
